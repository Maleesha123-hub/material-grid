package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceipt;
import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceiptRow;
import com.pixelMind.materialGrid.dto.response.ReceiptSummaryDTO;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleExpense;
import com.pixelMind.materialGrid.entity.VehicleLicense;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.LicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.DailyRouteReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * MANDATORY rule (unchanged): the Licence Fee lookup NEVER checks
 * License.startDate/endDate.
 *
 * Multiple DailyRoute records per vehicle+date are expected and are
 * CONSOLIDATED into one receipt row per date (see buildConsolidatedRows()),
 * including consolidating their (possibly distinct) routes into one
 * comma-joined routeCode list and their KM into one summed total - see
 * DailyRoutePaymentReceiptRow's Javadoc.
 *
 * Every method here is read-only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRouteReportServiceImpl implements DailyRouteReportService {

    private final VehicleRepository vehicleRepository;
    private final DailyRouteRepository dailyRouteRepository;
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final LicenseRepository licenseRepository;

    @Override
    @Transactional(readOnly = true)
    public DailyRoutePaymentReceipt generateReport(LocalDate startDate, LocalDate endDate, Long vehicleId) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    "Start date cannot be greater than end date.", ErrorCodeConstants.VALIDATION_FAILED);
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found.", ErrorCodeConstants.VEHICLE_NOT_FOUND));

        List<DailyRoute> dailyRoutes = dailyRouteRepository.findByVehicleIdAndDateBetween(vehicleId, startDate, endDate);
        if (dailyRoutes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No daily route data found for the selected vehicle and date range.",
                    ErrorCodeConstants.DAILY_ROUTE_NOT_FOUND);
        }

        Map<LocalDate, BigDecimal> paidAmountByDate = new HashMap<>();
        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        for (VehicleExpense expense : vehicleExpenseRepository.findByVehicleIdAndDateBetweenAndDeletedFalse(
                vehicleId, startDate, endDate)) {
            paidAmountByDate.merge(expense.getDate(), expense.getExpenses(), BigDecimal::add);
            totalPaidAmount = totalPaidAmount.add(expense.getExpenses());
        }

        List<DailyRoutePaymentReceiptRow> rows = buildConsolidatedRows(vehicle, dailyRoutes, paidAmountByDate);

        int totalLoadCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalKm = BigDecimal.ZERO;
        TreeSet<BigDecimal> distinctOverallRates = new TreeSet<>();
        for (DailyRoutePaymentReceiptRow row : rows) {
            totalLoadCount += row.getLoadCount();
            totalAmount = totalAmount.add(row.getTotalAmount());
            totalKm = totalKm.add(row.getTotalKm());
            distinctOverallRates.add(row.getPriceRate());
        }
        boolean priceRateVaries = distinctOverallRates.size() > 1;
        BigDecimal overallPriceRate = priceRateVaries ? null : distinctOverallRates.first();

        BigDecimal licenceFee = resolveLicenceFeeForRange(vehicle, startDate, endDate);
        BigDecimal balance = totalAmount.subtract(totalPaidAmount.add(licenceFee));

        log.info("Vehicle payment receipt generated: vehicleId={}, startDate={}, endDate={}, dateRows={}, "
                        + "totalLoadCount={}, totalKm={}, totalAmount={}, totalPaidAmount={}, licenceFee={}, balance={}",
                vehicleId, startDate, endDate, rows.size(), totalLoadCount, totalKm, totalAmount, totalPaidAmount,
                licenceFee, balance);

        return DailyRoutePaymentReceipt.builder()
                .vehicleNumber(vehicle.getVehicleNumber())
                .vehicleCapacity(vehicle.getCapacity())
                .startDate(startDate)
                .endDate(endDate)
                .rows(rows)
                .totalLoadCount(totalLoadCount)
                .totalKm(totalKm)
                .priceRate(overallPriceRate)
                .priceRateVaries(priceRateVaries)
                .totalAmount(totalAmount)
                .totalPaidAmount(totalPaidAmount)
                .licenceFee(licenceFee)
                .balance(balance)
                .build();
    }

    @Override
    public ReceiptSummaryDTO getSummary(LocalDate date, Long vehicleId) {
        return null;
    }

    /**
     * Consolidates raw DailyRoute records into one row per date. Grouping
     * uses a LinkedHashMap: `dailyRoutes` is already ordered by date
     * ascending (see the repository query), so first-insertion order into
     * the map equals ascending date order - no separate sort needed.
     */
    private List<DailyRoutePaymentReceiptRow> buildConsolidatedRows(
            Vehicle vehicle, List<DailyRoute> dailyRoutes, Map<LocalDate, BigDecimal> paidAmountByDate) {

        Map<LocalDate, List<DailyRoute>> byDate = new LinkedHashMap<>();
        for (DailyRoute dailyRoute : dailyRoutes) {
            byDate.computeIfAbsent(dailyRoute.getDate(), d -> new ArrayList<>()).add(dailyRoute);
        }

        List<DailyRoutePaymentReceiptRow> rows = new ArrayList<>(byDate.size());
        for (Map.Entry<LocalDate, List<DailyRoute>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<DailyRoute> recordsForDate = entry.getValue();

            BigDecimal totalAmountForDate = BigDecimal.ZERO;
            BigDecimal totalKmForDate = BigDecimal.ZERO;
            // TreeSet: BigDecimal.equals() is scale-sensitive, compareTo()
            // is not - this correctly treats equal price VALUES as one
            // entry regardless of stored scale.
            TreeSet<BigDecimal> distinctRatesForDate = new TreeSet<>();
            // LinkedHashSet: dedupes route codes while preserving the order
            // they were first encountered that day.
            Set<String> distinctRouteCodesForDate = new LinkedHashSet<>();

            for (DailyRoute dailyRoute : recordsForDate) {
                totalAmountForDate = totalAmountForDate.add(dailyRoute.getAmount());
                totalKmForDate = dailyRoute.getRoute().getKm();
                distinctRatesForDate.add(dailyRoute.getPriceRate().getPrice());
                distinctRouteCodesForDate.add(dailyRoute.getRoute().getRouteCode());
            }

            if (distinctRatesForDate.size() > 1) {
                throw new BusinessException(
                        "Multiple price rates found for vehicle " + vehicle.getVehicleNumber()
                                + " on date " + date + "; cannot determine a single price rate for this date.",
                        ErrorCodeConstants.AMBIGUOUS_PRICE_RATE);
            }

            rows.add(DailyRoutePaymentReceiptRow.builder()
                    .date(date)
                    .routeCode(String.join(", ", distinctRouteCodesForDate))
                    .totalKm(totalKmForDate)
                    .loadCount(recordsForDate.size())
                    .priceRate(distinctRatesForDate.first())
                    .totalAmount(totalAmountForDate)
                    .paidAmount(paidAmountByDate.getOrDefault(date, BigDecimal.ZERO))
                    .build());
        }
        return rows;
    }

    /** Unchanged from the previous iteration. */
    private BigDecimal resolveLicenceFeeForRange(Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        List<VehicleLicense> vehicleLicenses =
                vehicleLicenseRepository.findByVehicleIdAndDateBetween(vehicle.getId(), startDate, endDate);

        Set<Long> distinctActiveLicenseIds = new LinkedHashSet<>();
        for (VehicleLicense vl : vehicleLicenses) {
            if (vl.getStatus() == VehicleLicenseStatus.ACTIVE) {
                distinctActiveLicenseIds.add(vl.getLicense().getId());
            }
        }

        if (distinctActiveLicenseIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> resolvedForLogging = new HashSet<>();
        for (Long licenseId : distinctActiveLicenseIds) {
            License license = licenseRepository.findById(licenseId)
                    .orElseThrow(() -> {
                        log.error("Data integrity error: an ACTIVE VehicleLicense for vehicleId={} references "
                                + "nonexistent License id={}", vehicle.getId(), licenseId);
                        return new BusinessException(
                                "An active vehicle license for " + vehicle.getVehicleNumber()
                                        + " references a license that no longer exists. This indicates a data integrity issue.",
                                ErrorCodeConstants.DATA_INTEGRITY_ERROR);
                    });
            total = total.add(license.getPrice());
            resolvedForLogging.add(licenseId);
        }
        log.info("Licence fee resolved for vehicleId={}: distinct licenses={}, total={}",
                vehicle.getId(), resolvedForLogging, total);
        return total;
    }
}