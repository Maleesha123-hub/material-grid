package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleLicense;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * See VehicleExpenseImportServiceImpl for the rationale behind the
 * single-method @Transactional approach. Here it also covers requirement
 * "VehicleLicense updates must be in the same transaction as DailyRoute
 * inserts, and roll back together" - both mutations happen in this one
 * method, so a failure anywhere after the VehicleLicense saves rolls those
 * back too via Spring's standard unchecked-exception rollback.
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

    private record RawRow(int rowNumber, LocalDate date, String vehicleNumber, String routeCode, String checkBy) {
    }

    private record ResolvedRow(int rowNumber, LocalDate date, Vehicle vehicle, Route route,
                               VehicleLicense vehicleLicense, String checkBy) {
    }

    @Override
    @Transactional
    public BulkUploadResponse importFromExcel(MultipartFile file) {
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

            // --- Bulk-fetch vehicles and routes (one query each) ---
            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleRepository.findByVehicleNumberIn(distinctVehicleNumbers).stream()
                    .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            Set<String> distinctRouteCodes = rawRows.stream()
                    .map(RawRow::routeCode).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Route> routeByCode = routeRepository.findByRouteCodeIn(distinctRouteCodes).stream()
                    .collect(Collectors.toMap(Route::getRouteCode, r -> r));

            // --- Bulk-fetch licenses covering the file's whole date span (one query) ---
            List<LocalDate> validDates = rawRows.stream().map(RawRow::date).filter(d -> d != null).toList();
            List<License> candidateLicenses = List.of();
            if (!validDates.isEmpty()) {
                LocalDate minDate = validDates.stream().min(Comparator.naturalOrder()).get();
                LocalDate maxDate = validDates.stream().max(Comparator.naturalOrder()).get();
                candidateLicenses = licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(maxDate, minDate);
            }
            final List<License> licensesForRange = candidateLicenses;

            // --- Resolve vehicle, route, license per row ---
            record PartialRow(RawRow raw, Vehicle vehicle, Route route, License license) {
            }
            List<PartialRow> partials = new ArrayList<>();

            for (RawRow raw : rawRows) {
                Vehicle vehicle = null;
                Route route = null;
                License license = null;
                boolean rowHasError = false;

                if (!raw.vehicleNumber().isBlank()) {
                    vehicle = vehicleByNumber.get(raw.vehicleNumber());
                    if (vehicle == null) {
                        errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                                "Vehicle number '" + raw.vehicleNumber() + "' does not exist"));
                        rowHasError = true;
                    }
                } else {
                    rowHasError = true;
                }

                if (!raw.routeCode().isBlank()) {
                    route = routeByCode.get(raw.routeCode());
                    if (route == null) {
                        errors.add(error(raw.rowNumber(), "Route Code", raw.routeCode(),
                                "Route code '" + raw.routeCode() + "' does not exist"));
                        rowHasError = true;
                    }
                } else {
                    rowHasError = true;
                }

                if (raw.date() != null) {
                    List<License> matches = licensesForRange.stream()
                            .filter(l -> !l.getStartDate().isAfter(raw.date()) && !l.getEndDate().isBefore(raw.date()))
                            .toList();
                    if (matches.isEmpty()) {
                        errors.add(error(raw.rowNumber(), "Date", raw.date().toString(),
                                "No valid license found for date " + raw.date()));
                        rowHasError = true;
                    } else if (matches.size() > 1) {
                        errors.add(error(raw.rowNumber(), "Date", raw.date().toString(),
                                "Multiple valid licenses found for date " + raw.date() + "; cannot determine which to use"));
                        rowHasError = true;
                    } else {
                        license = matches.get(0);
                    }
                } else {
                    rowHasError = true;
                }

                if (raw.checkBy().isBlank()) {
                    rowHasError = true;
                }

                if (!rowHasError) {
                    partials.add(new PartialRow(raw, vehicle, route, license));
                }
            }

            // --- Bulk-fetch VehicleLicenses for every (vehicle, license) pair actually needed ---
            Set<Long> vehicleIds = partials.stream().map(p -> p.vehicle().getId()).collect(Collectors.toSet());
            Set<Long> licenseIds = partials.stream().map(p -> p.license().getId()).collect(Collectors.toSet());
            Map<String, VehicleLicense> vehicleLicenseByPair = new HashMap<>();
            if (!vehicleIds.isEmpty() && !licenseIds.isEmpty()) {
                for (VehicleLicense vl : vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(vehicleIds, licenseIds)) {
                    vehicleLicenseByPair.put(pairKey(vl.getVehicle().getId(), vl.getLicense().getId()), vl);
                }
            }

            List<ResolvedRow> resolved = new ArrayList<>();
            for (PartialRow p : partials) {
                String key = pairKey(p.vehicle().getId(), p.license().getId());
                VehicleLicense vehicleLicense = vehicleLicenseByPair.get(key);
                if (vehicleLicense == null) {
                    errors.add(error(p.raw().rowNumber(), "Vehicle License", null,
                            "No vehicle license found for vehicle '" + p.vehicle().getVehicleNumber()
                                    + "' and license '" + p.license().getLicenseCode() + "'"));
                    continue;
                }
                resolved.add(new ResolvedRow(p.raw().rowNumber(), p.raw().date(), p.vehicle(), p.route(),
                        vehicleLicense, p.raw().checkBy()));
            }

            // --- Duplicate detection: within the file, and against existing history ---
            Set<String> seenInFile = new HashSet<>();
            for (ResolvedRow row : resolved) {
                String businessKey = row.date() + "|" + row.vehicle().getId() + "|" + row.route().getId();
                if (!seenInFile.add(businessKey)) {
                    errors.add(error(row.rowNumber(), "Duplicate", null,
                            "Duplicate daily route in this file for date " + row.date() + ", vehicle '"
                                    + row.vehicle().getVehicleNumber() + "', route '" + row.route().getRouteCode() + "'"));
                }
            }

            if (!resolved.isEmpty()) {
                Set<LocalDate> dates = resolved.stream().map(ResolvedRow::date).collect(Collectors.toSet());
                Set<Long> vIds = resolved.stream().map(r -> r.vehicle().getId()).collect(Collectors.toSet());
                Set<Long> rIds = resolved.stream().map(r -> r.route().getId()).collect(Collectors.toSet());
                Set<String> existingKeys = dailyRouteRepository.findPotentialDuplicates(dates, vIds, rIds).stream()
                        .map(d -> d.getDate() + "|" + d.getVehicle().getId() + "|" + d.getRoute().getId())
                        .collect(Collectors.toSet());

                for (ResolvedRow row : resolved) {
                    String businessKey = row.date() + "|" + row.vehicle().getId() + "|" + row.route().getId();
                    if (existingKeys.contains(businessKey)) {
                        errors.add(error(row.rowNumber(), "Duplicate", null,
                                "A daily route already exists for date " + row.date() + ", vehicle '"
                                        + row.vehicle().getVehicleNumber() + "', route '" + row.route().getRouteCode() + "'"));
                    }
                }
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Daily route upload validation failed", errors, rawRows.size());
            }

            // --- All rows valid: resolve active price rate, then write ---
            PriceRate activePriceRate = priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(
                            "No active price rate is available.", ErrorCodeConstants.ACTIVE_PRICE_RATE_NOT_FOUND));

            String actor = SecurityUtil.getCurrentUsername();

            Map<Long, VehicleLicense> toActivate = new LinkedHashMap<>();
            for (ResolvedRow row : resolved) {
                VehicleLicense vl = row.vehicleLicense();
                if (vl.getStatus() == VehicleLicenseStatus.INACTIVE) {
                    vl.setStatus(VehicleLicenseStatus.ACTIVE);
                    vl.setDate(row.date());
                    vl.setModifiedBy(actor);
                    toActivate.put(vl.getId(), vl);
                }
            }
            if (!toActivate.isEmpty()) {
                vehicleLicenseRepository.saveAll(toActivate.values());
                log.info("Bulk daily route upload: activated {} vehicle license(s), by={}", toActivate.size(), actor);
            }

            List<DailyRoute> entities = (List<DailyRoute>) resolved.stream()
                    .map(row -> DailyRoute.builder()
                            .date(row.date())
                            .vehicle(row.vehicle())
                            .route(row.route())
                            .priceRate(activePriceRate)
                            .amount(row.route().getKm().multiply(activePriceRate.getPrice())
                                    .setScale(4, RoundingMode.HALF_UP))
                            .checkBy(row.checkBy())
                            .deleted(false)
                            .createdBy(actor)
                            .modifiedBy(actor)
                            .build())
                    .toList();

            dailyRouteRepository.saveAll(entities);
            log.info("Bulk daily route upload: {} rows inserted, by={}", entities.size(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Daily routes uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
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