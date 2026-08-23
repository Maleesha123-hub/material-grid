package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.FileHistoryService;
import com.pixelMind.materialGrid.service.VehicleImportService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mirrors VehicleExpenseImportServiceImpl's structure exactly (bulk lookup,
 * in-file + against-DB duplicate detection, collect-all-errors-then-write).
 * The only genuinely new piece versus that class is the File History
 * duplicate check at the very top, before the workbook is even opened - see
 * FileHistoryServiceImpl's class Javadoc for why createFileHistory() must
 * run inside this same @Transactional method (not REQUIRES_NEW).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleImportServiceImpl implements VehicleImportService {

    private static final Pattern VEHICLE_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9-]{4,20}$");

    private final VehicleRepository vehicleRepository;
    private final FileHistoryService fileHistoryService;

    private record RawRow(int rowNumber, String vehicleNumber, BigDecimal capacity) {
    }

    private record ResolvedRow(String vehicleNumber, BigDecimal capacity) {
    }

    @Override
    @Transactional
    public BulkUploadResponse importFromExcel(MultipartFile file) {
        String fileName = ExcelUtil.extractSafeFileName(file);
        fileHistoryService.validateNotAlreadyUploaded(fileName, FileType.VEHICLE);

        Workbook workbook = ExcelUtil.openWorkbook(file);
        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, ExcelConstants.VEHICLE_HEADERS);

            int vehicleNumberCol = ExcelUtil.columnOf(headerIndex, "Vehicle Number");
            int capacityCol = ExcelUtil.columnOf(headerIndex, "Capacity");

            List<ExcelValidationError> errors = new ArrayList<>();
            List<RawRow> rawRows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }
                int rowNumber = r + 1;

                String vehicleNumber = ExcelUtil.readString(row, vehicleNumberCol).toUpperCase();
                if (vehicleNumber.isBlank()) {
                    errors.add(error(rowNumber, "Vehicle Number", null, "Vehicle number is required"));
                } else if (!VEHICLE_NUMBER_PATTERN.matcher(vehicleNumber).matches()) {
                    errors.add(error(rowNumber, "Vehicle Number", vehicleNumber,
                            "Vehicle number must be 4-20 uppercase letters, digits, or hyphens"));
                }

                Optional<BigDecimal> capacity = ExcelUtil.readBigDecimal(row, capacityCol);
                if (capacity.isEmpty()) {
                    errors.add(error(rowNumber, "Capacity", ExcelUtil.readString(row, capacityCol),
                            "Capacity is required and must be a valid number"));
                } else if (capacity.get().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(error(rowNumber, "Capacity", capacity.get().toString(),
                            "Capacity must be greater than zero"));
                }

                rawRows.add(new RawRow(rowNumber, vehicleNumber, capacity.orElse(null)));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            // Duplicate vehicle numbers WITHIN this file.
            Set<String> seenInFile = new HashSet<>();
            for (RawRow raw : rawRows) {
                if (raw.vehicleNumber().isBlank()) {
                    continue;
                }
                if (!seenInFile.add(raw.vehicleNumber())) {
                    errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                            "Duplicate vehicle number '" + raw.vehicleNumber() + "' within this file"));
                }
            }

            // Vehicle numbers that already exist in the database - one bulk
            // query for every distinct number in the file.
            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Set<String> existingVehicleNumbers = vehicleRepository.findByVehicleNumberInAndDeletedFalse(distinctVehicleNumbers)
                    .stream().map(Vehicle::getVehicleNumber).collect(Collectors.toSet());

            List<ResolvedRow> resolved = new ArrayList<>();
            for (RawRow raw : rawRows) {
                if (raw.vehicleNumber().isBlank() || !VEHICLE_NUMBER_PATTERN.matcher(raw.vehicleNumber()).matches()
                        || raw.capacity() == null) {
                    continue; // already reported above
                }
                if (existingVehicleNumbers.contains(raw.vehicleNumber())) {
                    errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                            "Vehicle number '" + raw.vehicleNumber() + "' already exists"));
                    continue;
                }
                resolved.add(new ResolvedRow(raw.vehicleNumber(), raw.capacity()));
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Vehicle upload validation failed", errors, rawRows.size());
            }

            // All rows valid - create File History, then the Vehicles, in
            // this same transaction (see class Javadoc).
            FileHistory fileHistory = fileHistoryService.createFileHistory(fileName, FileType.VEHICLE);
            String actor = SecurityUtil.getCurrentUsername();

            List<Vehicle> entities = (List<Vehicle>) resolved.stream()
                    .map(r -> Vehicle.builder()
                            .vehicleNumber(r.vehicleNumber())
                            .capacity(r.capacity())
                            .fileHistory(fileHistory)
                            .createdBy(actor)
                            .modifiedBy(actor)
                            .build())
                    .toList();

            vehicleRepository.saveAll(entities);
            log.info("Bulk vehicle upload: {} rows inserted, fileHistoryId={}, by={}",
                    entities.size(), fileHistory.getId(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Vehicles uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
                    .fileHistoryId(fileHistory.getId())
                    .fileName(fileName)
                    .fileType(FileType.VEHICLE.name())
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