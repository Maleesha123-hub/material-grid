package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleLicense;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.LicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.FileHistoryService;
import com.pixelMind.materialGrid.service.VehicleLicenseImportService;
import com.pixelMind.materialGrid.util.ExcelUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mirrors VehicleImportServiceImpl / DailyRouteImportServiceImpl exactly:
 * duplicate-file check before parsing, bulk lookups instead of per-row
 * queries, collect-all-errors-then-write, FileHistory created (in this same
 * transaction - see FileHistoryServiceImpl's Javadoc) only once every row
 * has passed.
 *
 * DEFAULT STATUS DECISION: newly created VehicleLicense rows from this
 * import are set to INACTIVE. Nothing elsewhere in the project defines a
 * default for a NEWLY CREATED VehicleLicense (the manual CRUD create
 * endpoint requires the client to supply status explicitly; the DailyRoute
 * importer only ever flips an EXISTING row from INACTIVE to ACTIVE, it
 * never creates new rows). INACTIVE mirrors that same idea - a bulk-created
 * assignment hasn't yet been verified/put to use, and only becomes ACTIVE
 * when actually exercised (e.g. via the DailyRoute import's activation
 * logic). This is a judgment call, flagged explicitly rather than silently
 * assumed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleLicenseImportServiceImpl implements VehicleLicenseImportService {

    private final VehicleRepository vehicleRepository;
    private final LicenseRepository licenseRepository;
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final FileHistoryService fileHistoryService;

    private record RawRow(int rowNumber, String vehicleNumber, String licenseCode) {
    }

    private record ResolvedRow(int rowNumber, Vehicle vehicle, License license) {
    }

    @Override
    @Transactional
    public BulkUploadResponse importFromExcel(MultipartFile file) {
        String fileName = ExcelUtil.extractSafeFileName(file);
        fileHistoryService.validateNotAlreadyUploaded(fileName, FileType.VEHICLE_LICENSE);

        Workbook workbook = ExcelUtil.openWorkbook(file);
        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, ExcelConstants.VEHICLE_LICENSE_HEADERS);

            int vehicleCol = ExcelUtil.columnOf(headerIndex, "Vehicle Number");
            int licenseCol = ExcelUtil.columnOf(headerIndex, "License Code");

            List<ExcelValidationError> errors = new ArrayList<>();
            List<RawRow> rawRows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }
                int rowNumber = r + 1;

                String vehicleNumber = ExcelUtil.readString(row, vehicleCol).toUpperCase();
                if (vehicleNumber.isBlank()) {
                    errors.add(error(rowNumber, "Vehicle Number", null, "Vehicle Number is required"));
                }

                String licenseCode = ExcelUtil.readString(row, licenseCol).toUpperCase();
                if (licenseCode.isBlank()) {
                    errors.add(error(rowNumber, "License Code", null, "License Code is required"));
                }

                rawRows.add(new RawRow(rowNumber, vehicleNumber, licenseCode));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            // --- Bulk-fetch vehicles and licenses (one query each) ---
            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleRepository.findByVehicleNumberInAndDeletedFalse(distinctVehicleNumbers).stream()
                    .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            Set<String> distinctLicenseCodes = rawRows.stream()
                    .map(RawRow::licenseCode).filter(l -> !l.isBlank()).collect(Collectors.toSet());
            Map<String, License> licenseByCode = licenseRepository.findByLicenseCodeInAndDeletedFalse(distinctLicenseCodes).stream()
                    .collect(Collectors.toMap(License::getLicenseCode, l -> l));

            List<ResolvedRow> resolved = new ArrayList<>();
            for (RawRow raw : rawRows) {
                if (raw.vehicleNumber().isBlank() || raw.licenseCode().isBlank()) {
                    continue;
                }

                Vehicle vehicle = vehicleByNumber.get(raw.vehicleNumber());
                if (vehicle == null) {
                    errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                            "Vehicle number '" + raw.vehicleNumber() + "' does not exist"));
                    continue;
                }

                License license = licenseByCode.get(raw.licenseCode());
                if (license == null) {
                    errors.add(error(raw.rowNumber(), "License Code", raw.licenseCode(),
                            "License code '" + raw.licenseCode() + "' does not exist"));
                    continue;
                }

                resolved.add(new ResolvedRow(raw.rowNumber(), vehicle, license));
            }

            // --- Duplicate detection WITHIN this file ---
            Set<String> seenInFile = new HashSet<>();
            for (ResolvedRow row : resolved) {
                String key = businessKey(row.vehicle().getId(), row.license().getId());
                if (!seenInFile.add(key)) {
                    errors.add(error(row.rowNumber(), "Duplicate", null,
                            "Duplicate Vehicle License record found in the uploaded file. The same license "
                                    + "is already provided for the same vehicle."));
                }
            }

            // --- Duplicate detection against EXISTING database records ---
            if (!resolved.isEmpty()) {
                Set<Long> vehicleIds = resolved.stream().map(r -> r.vehicle().getId()).collect(Collectors.toSet());
                Set<Long> licenseIds = resolved.stream().map(r -> r.license().getId()).collect(Collectors.toSet());

                Set<String> existingKeys = vehicleLicenseRepository
                        .findByVehicleIdInAndLicenseIdInAndDeletedFalse(vehicleIds, licenseIds).stream()
                        .map(vl -> businessKey(vl.getVehicle().getId(), vl.getLicense().getId()))
                        .collect(Collectors.toSet());

                for (ResolvedRow row : resolved) {
                    String key = businessKey(row.vehicle().getId(), row.license().getId());
                    if (existingKeys.contains(key)) {
                        errors.add(error(row.rowNumber(), "Vehicle License", null,
                                "License is already available for the provided Vehicle."));
                    }
                }
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Vehicle license upload validation failed", errors, rawRows.size());
            }

            FileHistory fileHistory = fileHistoryService.createFileHistory(fileName, FileType.VEHICLE_LICENSE);
            String actor = SecurityUtil.getCurrentUsername();

            List<VehicleLicense> entities = resolved.stream()
                    .<VehicleLicense>map(r -> VehicleLicense.builder()
                            .vehicle(r.vehicle())
                            .license(r.license())
                            .date(null)
                            .status(VehicleLicenseStatus.ACTIVE)
                            .fileHistory(fileHistory)
                            .createdBy(actor)
                            .modifiedBy(actor)
                            .build())
                    .toList();

            vehicleLicenseRepository.saveAll(entities);
            log.info("Bulk vehicle license upload: {} rows inserted, fileHistoryId={}, by={}",
                    entities.size(), fileHistory.getId(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Vehicle licenses uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
                    .fileHistoryId(fileHistory.getId())
                    .fileName(fileName)
                    .fileType(FileType.VEHICLE_LICENSE.name())
                    .build();
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String businessKey(Long vehicleId, Long licenseId) {
        return vehicleId + ":" + licenseId;
    }

    private ExcelValidationError error(int rowNumber, String field, String value, String message) {
        return ExcelValidationError.builder().rowNumber(rowNumber).field(field).value(value).message(message).build();
    }
}