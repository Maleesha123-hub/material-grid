package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.*;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.*;
import com.pixelMind.materialGrid.service.DailyRouteImportService;
import com.pixelMind.materialGrid.service.FileHistoryService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MODIFIED: same File History wiring pattern as VehicleExpenseImportServiceImpl
 * - duplicate-file check before parsing, FileHistory created (in this same
 * transaction) only after every row + business rule passes, then tagged
 * onto every created DailyRoute alongside the existing VehicleLicense
 * activation logic. Nothing about the existing validation/duplicate-
 * detection/rollback behavior changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRouteImportServiceImpl implements DailyRouteImportService {

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final LicenseRepository licenseRepository;
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final DailyRouteRepository dailyRouteRepository;
    private final PriceRateRepository priceRateRepository;
    private final FileHistoryService fileHistoryService;

    private record RawRow(int rowNumber, LocalDate date, String vehicleNumber, String bilNumber, String routeCode) {
    }

    private record ResolvedRow(int rowNumber, LocalDate date, Vehicle vehicle, String bilNumber, Route route) {
    }

    @Override
    @Transactional
    public BulkUploadResponse importFromExcel(MultipartFile file) {
        String fileName = ExcelUtil.extractSafeFileName(file);
        fileHistoryService.validateNotAlreadyUploaded(fileName, FileType.DAILY_ROUTE);

        Workbook workbook = ExcelUtil.openWorkbook(file);
        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, ExcelConstants.DAILY_ROUTE_HEADERS);

            int dateCol = ExcelUtil.columnOf(headerIndex, "Date");
            int vehicleCol = ExcelUtil.columnOf(headerIndex, "Vehicle Number");
            int bilNumberCol = ExcelUtil.columnOf(headerIndex, "Bil Number");
            int routeCol = ExcelUtil.columnOf(headerIndex, "Route Code");

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

                String bilNumber = ExcelUtil.readString(row, bilNumberCol);
                if (bilNumber.isBlank()) {
                    errors.add(error(rowNumber, "Bil Number", ExcelUtil.readString(row, bilNumberCol),
                            "Bil Number is required"));
                }

                String routeCode = ExcelUtil.readString(row, routeCol).toUpperCase();
                if (routeCode.isBlank()) {
                    errors.add(error(rowNumber, "Route Code", null, "Route code is required"));
                }

                rawRows.add(new RawRow(rowNumber, date.orElse(null), vehicleNumber, bilNumber, routeCode));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleRepository.findByVehicleNumberInAndDeletedFalse(distinctVehicleNumbers).stream()
                    .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            Set<String> distinctRouteCodes = rawRows.stream()
                    .map(RawRow::routeCode).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Route> routeByCode = routeRepository.findByRouteCodeInAndDeletedFalse(distinctRouteCodes).stream()
                    .collect(Collectors.toMap(Route::getRouteCode, r -> r));

//            List<LocalDate> validDates = rawRows.stream().map(RawRow::date).filter(Objects::nonNull).toList();
//            List<License> candidateLicenses = List.of();
//            if (!validDates.isEmpty()) {
//                LocalDate minDate = validDates.stream().min(Comparator.naturalOrder()).get();
//                LocalDate maxDate = validDates.stream().max(Comparator.naturalOrder()).get();
//                candidateLicenses = licenseRepository.findByDateRange(maxDate, minDate);
//            }
//            final List<License> licensesForRange = candidateLicenses;

            List<ResolvedRow> resolved = new ArrayList<>();
            for (RawRow raw : rawRows) {

                Vehicle vehicle = vehicleByNumber.get(raw.vehicleNumber());
                if (vehicle == null) {
                    errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                            "Vehicle number '" + raw.vehicleNumber() + "' does not exist"));
                    continue;
                }

                Route route = routeByCode.get(raw.routeCode());
                if (route == null) {
                    errors.add(error(raw.rowNumber(), "Route Code", raw.routeCode(),
                            "Route code '" + raw.routeCode() + "' does not exist"));
                    continue;
                }

                resolved.add(new ResolvedRow(raw.rowNumber, raw.date, vehicle, raw.bilNumber(), route));
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Daily route upload validation failed", errors, rawRows.size());
            }

            FileHistory fileHistory = fileHistoryService.createFileHistory(fileName, FileType.DAILY_ROUTE);
            String actor = SecurityUtil.getCurrentUsername();

//            resolved.stream()
//                    .filter(row -> !vehicleLicenseRepository.existsByVehicleIdAndLicenseIdAndDeletedFalse(row.vehicle.getId(), row.license.getId()))
//                            .forEach(row -> {
//                                VehicleLicense vehicleLicense = VehicleLicense.builder()
//                                        .vehicle(row.vehicle)
//                                        .license(row.license)
//                                        .date(row.date)
//                                        .status(VehicleLicenseStatus.ACTIVE)
//                                        .createdBy(actor)
//                                        .modifiedBy(actor)
//                                        .build();
//                                VehicleLicense saved = vehicleLicenseRepository.save(vehicleLicense);
//                                log.info("Bulk daily route upload: activated {} vehicle license, by={}", saved.getId(), actor);
//                            });

            resolved.forEach(raw -> {

                if (!licenseRepository.existsActiveLicenseByDate(raw.date)) { // TODO: this impl is also exists in Daily route create / update

                    errors.add(error(raw.rowNumber(), "Date", String.valueOf(raw.date),
                            "Valid license does not exists for the daily route date"));

                } else {

                    List<License> licenses = licenseRepository.findAllActiveLicensesByDate(raw.date);
                    List<VehicleLicense> vehicleLicenses = vehicleLicenseRepository.findByVehicleAndLicenseInAndDeletedFalse(raw.vehicle, licenses);

                    if (vehicleLicenses.isEmpty()) {

                        errors.add(
                                error(
                                        raw.rowNumber(), "Vehicle Number", raw.vehicle.getVehicleNumber(),
                                        "Vehicle license does not exists for the vehicle, Please assign a vehicle license for the vehicle : " +
                                                raw.vehicle.getVehicleNumber()
                                )
                        );

                    } else if (vehicleLicenses.size() > 1) {

                        errors.add(
                                error(
                                        raw.rowNumber(), "Vehicle Number", raw.vehicle.getVehicleNumber(),
                                        "Multiple vehicle license assigned for the vehicle : " +
                                                raw.vehicle.getVehicleNumber()
                                )
                        );

                    } else if (vehicleLicenses.getFirst().getDate() == null) {

                        VehicleLicense vehicleLicense = vehicleLicenses.getFirst();
                        vehicleLicense.setDate(raw.date);
                        try {

                            vehicleLicenseRepository.save(vehicleLicense);

                        } catch (Exception ex) {

                            log.error("Daily route upload validation failed : {}", ex.getMessage(), ex);

                            throw new ExcelValidationException("Vehicle license save failed : " + ex.getMessage(), errors, rawRows.size());

                        }

                    }

                }

            });

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Daily route upload validation failed", errors, rawRows.size());
            }

            List<DailyRoute> entities = (List<DailyRoute>) resolved.stream()
                    .map(row -> DailyRoute.builder()
                            .date(row.date())
                            .vehicle(row.vehicle())
                            .route(row.route())
                            .amount(computeAmount(row.vehicle(), row.route()))
                            .billNumber(row.bilNumber())
                            .fileHistory(fileHistory)
                            .deleted(false)
                            .createdBy(actor)
                            .modifiedBy(actor)
                            .build())
                    .toList();

            dailyRouteRepository.saveAll(entities);
            log.info("Bulk daily route upload: {} rows inserted, fileHistoryId={}, by={}",
                    entities.size(), fileHistory.getId(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Daily routes uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
                    .fileHistoryId(fileHistory.getId())
                    .fileName(fileName)
                    .fileType(FileType.DAILY_ROUTE.name())
                    .build();
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String pairKey(Long vehicleId, Long licenseId) {
        return vehicleId + ":" + licenseId;
    }

    private ExcelValidationError error(int rowNumber, String field, String value, String message) {
        return ExcelValidationError.builder().rowNumber(rowNumber).field(field).value(value).message(message).build();
    }

    private BigDecimal computeAmount(Vehicle vehicle, Route route) {
        return vehicle.getCapacity()
                .multiply(route.getPrice())
                .multiply(route.getKm())
                .setScale(2, RoundingMode.HALF_UP);
    }
}