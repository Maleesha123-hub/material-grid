package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleExpense;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.FileHistoryService;
import com.pixelMind.materialGrid.service.VehicleExpenseImportService;
import com.pixelMind.materialGrid.util.ExcelUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MODIFIED: now depends on FileHistoryService and performs the duplicate-
 * file check (fileName + VEHICLE_EXPENSE) before opening the workbook, and
 * creates the FileHistory row (in this same transaction - see
 * FileHistoryServiceImpl's Javadoc) immediately before saving the validated
 * VehicleExpense rows, tagging every one of them with that FileHistory.
 * All prior validation/bulk-lookup/all-or-nothing behavior is unchanged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpenseImportServiceImpl implements VehicleExpenseImportService {

    private final VehicleRepository vehicleRepository;
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final FileHistoryService fileHistoryService;

    private record RawRow(int rowNumber, LocalDate date, String vehicleNumber, BigDecimal expense) {
    }

    private record ResolvedRow(LocalDate date, Vehicle vehicle, BigDecimal expense) {
    }

    @Override
    @Transactional
    public BulkUploadResponse importFromExcel(MultipartFile file) {
        String fileName = ExcelUtil.extractSafeFileName(file);
        fileHistoryService.validateNotAlreadyUploaded(fileName, FileType.VEHICLE_EXPENSE);

        Workbook workbook = ExcelUtil.openWorkbook(file);
        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, ExcelConstants.VEHICLE_EXPENSE_HEADERS);

            int dateCol = ExcelUtil.columnOf(headerIndex, "Date");
            int vehicleCol = ExcelUtil.columnOf(headerIndex, "Vehicle Number");
            int expenseCol = ExcelUtil.columnOf(headerIndex, "Expense");

            List<ExcelValidationError> errors = new ArrayList<>();
            List<RawRow> rawRows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }
                int rowNumber = r + 1;

                Optional<LocalDate> date = ExcelUtil.readDate(row, dateCol);
                if (date.isEmpty()) {
                    errors.add(error(rowNumber, "Date", ExcelUtil.readString(row, dateCol),
                            "Date is required and must be a valid date"));
                }

                String vehicleNumber = ExcelUtil.readString(row, vehicleCol).toUpperCase();
                if (vehicleNumber.isBlank()) {
                    errors.add(error(rowNumber, "Vehicle Number", null, "Vehicle number is required"));
                }

                Optional<BigDecimal> expense = ExcelUtil.readBigDecimal(row, expenseCol);
                if (expense.isEmpty()) {
                    errors.add(error(rowNumber, "Expense", ExcelUtil.readString(row, expenseCol),
                            "Expense is required and must be a valid number"));
                } else if (expense.get().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(error(rowNumber, "Expense", expense.get().toString(),
                            "Expense must be greater than zero"));
                }

                rawRows.add(new RawRow(rowNumber, date.orElse(null), vehicleNumber, expense.orElse(null)));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber)
                    .filter(v -> !v.isBlank())
                    .collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleRepository.findByVehicleNumberInAndDeletedFalse(distinctVehicleNumbers).stream()
                    .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            List<ResolvedRow> resolved = new ArrayList<>();
            for (RawRow raw : rawRows) {
                if (raw.vehicleNumber().isBlank()) {
                    continue;
                }
                Vehicle vehicle = vehicleByNumber.get(raw.vehicleNumber());
                if (vehicle == null) {
                    errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                            "Vehicle number '" + raw.vehicleNumber() + "' does not exist"));
                    continue;
                }
                if (raw.date() == null || raw.expense() == null) {
                    continue;
                }
                resolved.add(new ResolvedRow(raw.date(), vehicle, raw.expense()));
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Vehicle expense upload validation failed", errors, rawRows.size());
            }

            FileHistory fileHistory = fileHistoryService.createFileHistory(fileName, FileType.VEHICLE_EXPENSE);
            String actor = SecurityUtil.getCurrentUsername();

            List<VehicleExpense> entities = (List<VehicleExpense>) resolved.stream()
                    .map(r -> VehicleExpense.builder()
                            .date(r.date())
                            .expenses(r.expense())
                            .vehicle(r.vehicle())
                            .fileHistory(fileHistory)
                            .deleted(false)
                            .createdBy(actor)
                            .modifiedBy(actor)
                            .build())
                    .toList();

            vehicleExpenseRepository.saveAll(entities);
            log.info("Bulk vehicle expense upload: {} rows inserted, fileHistoryId={}, by={}",
                    entities.size(), fileHistory.getId(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Vehicle expenses uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
                    .fileHistoryId(fileHistory.getId())
                    .fileName(fileName)
                    .fileType(FileType.VEHICLE_EXPENSE.name())
                    .build();
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private ExcelValidationError error(int rowNumber, String field, String value, String message) {
        return ExcelValidationError.builder().rowNumber(rowNumber).field(field).value(value).message(message).build();
    }
}