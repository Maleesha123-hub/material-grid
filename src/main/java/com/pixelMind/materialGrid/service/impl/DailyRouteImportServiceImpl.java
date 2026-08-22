package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleLicense;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.LicenseRepository;
import com.pixelMind.materialGrid.repository.PriceRateRepository;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
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

    private record RawRow(int rowNumber, LocalDate date, String vehicleNumber, String routeCode, String checkBy) {
    }

    private record ResolvedRow(LocalDate date, Vehicle vehicle, Route route, License license, String checkBy) {
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
            int routeCol = ExcelUtil.columnOf(headerIndex, "Route Code");
            int checkByCol = ExcelUtil.columnOf(headerIndex, "Check By");

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

                String routeCode = ExcelUtil.readString(row, routeCol).toUpperCase();
                if (routeCode.isBlank()) {
                    errors.add(error(rowNumber, "Route Code", null, "Route code is required"));
                }

                String checkBy = ExcelUtil.readString(row, checkByCol);
                if (checkBy.isBlank()) {
                    errors.add(error(rowNumber, "Check By", null, "Check By is required"));
                }

                rawRows.add(new RawRow(rowNumber, date.orElse(null), vehicleNumber, routeCode, checkBy));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleRepository.findByVehicleNumberIn(distinctVehicleNumbers).stream()
                    .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            Set<String> distinctRouteCodes = rawRows.stream()
                    .map(RawRow::routeCode).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Route> routeByCode = routeRepository.findByRouteCodeIn(distinctRouteCodes).stream()
                    .collect(Collectors.toMap(Route::getRouteCode, r -> r));

            List<LocalDate> validDates = rawRows.stream().map(RawRow::date).filter(Objects::nonNull).toList();
            List<License> candidateLicenses = List.of();
            if (!validDates.isEmpty()) {
                LocalDate minDate = validDates.stream().min(Comparator.naturalOrder()).get();
                LocalDate maxDate = validDates.stream().max(Comparator.naturalOrder()).get();
                candidateLicenses = licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(maxDate, minDate);
            }
            final List<License> licensesForRange = candidateLicenses;

            List<ResolvedRow> resolved = new ArrayList<>();

            for (RawRow raw : rawRows) {
                License license;

                Vehicle vehicle = vehicleByNumber.get(raw.vehicleNumber());
                    if (vehicle == null) {
                        errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                                "Vehicle number '" + raw.vehicleNumber() + "' does not exist"));
                        continue;
                    }

                Route  route = routeByCode.get(raw.routeCode());
                    if (route == null) {
                        errors.add(error(raw.rowNumber(), "Route Code", raw.routeCode(),
                                "Route code '" + raw.routeCode() + "' does not exist"));
                        continue;
                    }

                    List<License> matches = licensesForRange.stream()
                            .filter(l -> !l.getStartDate().isAfter(raw.date()) && !l.getEndDate().isBefore(raw.date()))
                            .toList();
                    if (matches.isEmpty()) {
                        errors.add(error(raw.rowNumber(), "Date", raw.date().toString(),
                                "No valid license found for date " + raw.date()));
                        continue;
                    } else if (matches.size() > 1) {
                        errors.add(error(raw.rowNumber(), "Date", raw.date().toString(),
                                "Multiple valid licenses found for date " + raw.date() + "; cannot determine which to use"));
                        continue;
                    } else {
                        license = matches.getFirst();
                    }

                if (raw.checkBy().isBlank()) {
                    continue;
                }

                resolved.add(new ResolvedRow(raw.date, vehicle, route, license, raw.checkBy()));

            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Daily route upload validation failed", errors, rawRows.size());
            }

            PriceRate activePriceRate = priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(
                            "No active price rate is available.", ErrorCodeConstants.ACTIVE_PRICE_RATE_NOT_FOUND));

            FileHistory fileHistory = fileHistoryService.createFileHistory(fileName, FileType.DAILY_ROUTE);
            String actor = SecurityUtil.getCurrentUsername();

            resolved.stream()
                    .filter(row -> !vehicleLicenseRepository.existsByVehicleIdAndLicenseId(row.vehicle.getId(), row.license.getId()))
                            .forEach(row -> {
                                VehicleLicense vehicleLicense = VehicleLicense.builder()
                                        .vehicle(row.vehicle)
                                        .license(row.license)
                                        .date(row.date)
                                        .status(VehicleLicenseStatus.ACTIVE)
                                        .createdBy(actor)
                                        .modifiedBy(actor)
                                        .build();
                                VehicleLicense saved = vehicleLicenseRepository.save(vehicleLicense);
                                log.info("Bulk daily route upload: activated {} vehicle license, by={}", saved.getId(), actor);
                            });

            List<DailyRoute> entities = (List<DailyRoute>) resolved.stream()
                    .map(row -> DailyRoute.builder()
                            .date(row.date())
                            .vehicle(row.vehicle())
                            .route(row.route())
                            .priceRate(activePriceRate)
                            .amount(row.route().getKm().multiply(activePriceRate.getPrice())
                                    .setScale(4, RoundingMode.HALF_UP))
                            .checkBy(row.checkBy())
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
}