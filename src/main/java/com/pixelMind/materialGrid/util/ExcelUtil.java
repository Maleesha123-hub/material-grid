package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Low-level, feature-agnostic Apache POI helpers shared by every Excel
 * import service. Domain-specific meaning (which headers are required, what
 * counts as an invalid row) stays out of this class deliberately, so it can
 * be reused without coupling Vehicle Expense and Daily Route imports
 * together.
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static Workbook openWorkbook(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw structuralError("No file was uploaded, or the uploaded file is empty.");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null || !ExcelConstants.ALLOWED_EXCEL_EXTENSIONS.contains(extension.toLowerCase())) {
            throw structuralError("Unsupported file type. Expected .xlsx or .xls, got: " + file.getOriginalFilename());
        }
        try (InputStream in = file.getInputStream()) {
            return WorkbookFactory.create(in);
        } catch (IOException | RuntimeException e) {
            throw structuralError("The uploaded file could not be opened as a valid Excel workbook.");
        }
    }

    public static Sheet firstSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw structuralError("The uploaded workbook does not contain any sheets.");
        }
        return workbook.getSheetAt(0);
    }

    public static Map<String, Integer> readHeaderIndex(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw structuralError("The uploaded file does not contain a header row.");
        }
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
            String raw = readString(headerRow, c);
            if (!raw.isBlank()) {
                index.put(normalize(raw), c);
            }
        }
        return index;
    }

    public static void requireHeaders(Map<String, Integer> headerIndex, List<String> requiredHeaders) {
        List<String> missing = requiredHeaders.stream()
                .filter(h -> !headerIndex.containsKey(normalize(h)))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw structuralError("Missing required column(s): " + String.join(", ", missing)
                    + ". Expected headers: " + String.join(", ", requiredHeaders));
        }
    }

    public static int columnOf(Map<String, Integer> headerIndex, String headerName) {
        return headerIndex.get(normalize(headerName));
    }

    public static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            if (!readString(row, c).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static String readString(Row row, int colIndex) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    public static Optional<LocalDate> readDate(Row row, int colIndex) {
        if (row == null) {
            return Optional.empty();
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return Optional.empty();
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return Optional.of(cell.getLocalDateTimeCellValue().toLocalDate());
            }
            String raw = readString(row, colIndex);
            if (raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<BigDecimal> readBigDecimal(Row row, int colIndex) {
        if (row == null) {
            return Optional.empty();
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return Optional.empty();
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return Optional.of(BigDecimal.valueOf(cell.getNumericCellValue()));
            }
            String raw = readString(row, colIndex);
            if (raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(raw));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : null;
    }

    private static ExcelValidationException structuralError(String message) {
        ExcelValidationError error = ExcelValidationError.builder()
                .rowNumber(0)
                .field("File")
                .message(message)
                .build();
        return new ExcelValidationException(message, List.of(error), 0);
    }
}