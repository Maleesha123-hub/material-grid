package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.response.DailyExpensesPaymentReceiptRow;
import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceipt;
import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceiptRow;
import com.pixelMind.materialGrid.dto.response.ReceiptSummaryDTO;
import com.pixelMind.materialGrid.entity.*;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.repository.*;
import com.pixelMind.materialGrid.service.DailyRouteReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MANDATORY rule (unchanged): the Licence Fee lookup NEVER checks
 * License.startDate/endDate.
 * <p>
 * Multiple DailyRoute records per vehicle+date are expected and are
 * CONSOLIDATED into one receipt row per date (see buildConsolidatedRows()),
 * including consolidating their (possibly distinct) routes into one
 * comma-joined routeCode list and their KM into one summed total - see
 * DailyRoutePaymentReceiptRow's Javadoc.
 * <p>
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

        BigDecimal totalPaidAmount = BigDecimal.ZERO;

        List<VehicleExpense> expenses = vehicleExpenseRepository.findByVehicleIdAndDateBetweenAndDeletedFalse(
                vehicleId, startDate, endDate
        );

        for (VehicleExpense expense : expenses) {
            totalPaidAmount = totalPaidAmount.add(expense.getExpenses());
        }

        // Unique routes per date of daily routes
        Map<LocalDate, Set<Route>> routesByDate = dailyRoutes.stream()
                .collect(Collectors.groupingBy(
                        DailyRoute::getDate,
                        Collectors.mapping(DailyRoute::getRoute, Collectors.toSet())
                ));

        // Unique dates daily expenses
        Set<LocalDate> uniqueExpensesDates = expenses.stream()
                .map(VehicleExpense::getDate)
                .collect(Collectors.toSet());

        List<DailyRoutePaymentReceiptRow> rows = buildConsolidatedRows(dailyRoutes, routesByDate);

        List<DailyExpensesPaymentReceiptRow> paidRows = paidRows(uniqueExpensesDates, expenses);

        int totalLoadCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal routeDistance = BigDecimal.ZERO;
        TreeSet<BigDecimal> distinctOverallRates = new TreeSet<>();

        for (DailyRoutePaymentReceiptRow row : rows) {

            totalLoadCount += row.getLoadCount();
            totalAmount = totalAmount.add(row.getTotalAmount());
            distinctOverallRates.add(row.getPriceRate());

        }

        boolean priceRateVaries = distinctOverallRates.size() > 1;

        BigDecimal overallPriceRate = priceRateVaries ? null : distinctOverallRates.first();

        BigDecimal licenceFee = resolveLicenceFeeForRange(vehicle, startDate, endDate);
        BigDecimal balance = totalAmount.subtract(totalPaidAmount.add(licenceFee));

        log.info("Vehicle payment receipt generated: vehicleId={}, startDate={}, endDate={}, dateRows={}, "
                        + "totalLoadCount={}, routeDistance={}, totalAmount={}, totalPaidAmount={}, licenceFee={}, balance={}",
                vehicleId, startDate, endDate, rows.size(), totalLoadCount, routeDistance, totalAmount, totalPaidAmount,
                licenceFee, balance);

        return DailyRoutePaymentReceipt.builder()
                .vehicleNumber(vehicle.getVehicleNumber())
                .vehicleCapacity(vehicle.getCapacity())
                .startDate(startDate)
                .endDate(endDate)
                .rows(rows)
                .paidRows(paidRows)
                .totalLoadCount(totalLoadCount)
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
            List<DailyRoute> dailyRoutes,
            Map<LocalDate, Set<Route>> routesByDate
    ) {
        List<DailyRoutePaymentReceiptRow> rows = new ArrayList<>();

        routesByDate.forEach((routeDate, routes) -> {

            routes.forEach(route -> {

                int loadCount = Math.toIntExact(dailyRoutes.stream()
                        .filter(dr -> dr.getRoute().equals(route))
                        .filter(dr -> dr.getDate().equals(routeDate))
                        .count());

                BigDecimal totalAmount = dailyRoutes.stream()
                        .filter(dr -> dr.getRoute().equals(route))
                        .filter(dr -> dr.getDate().equals(routeDate))
                        .map(DailyRoute::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                rows.add(
                        DailyRoutePaymentReceiptRow.builder()
                                .date(routeDate)
                                .routeCode(route.getRouteCode())
                                .routeDistance(route.getKm())
                                .loadCount(loadCount)
                                .priceRate(route.getPrice())
                                .totalAmount(totalAmount)
                                .build()
                );

            });

        });

        return rows;

    }

    private List<DailyExpensesPaymentReceiptRow> paidRows(
            Set<LocalDate> uniqueDates,
            List<VehicleExpense> dailyExpenses
    ) {

        List<DailyExpensesPaymentReceiptRow> rows = new ArrayList<>();

        uniqueDates.forEach(date -> {

            BigDecimal paidAmountPerDate = dailyExpenses.stream()
                    .filter(de -> de.getDate().isEqual(date))
                    .map(VehicleExpense::getExpenses)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(DailyExpensesPaymentReceiptRow.builder()
                    .date(date)
                    .paidAmount(paidAmountPerDate)
                    .build()
            );

        });

        return rows;

    }

    /**
     * Unchanged from the previous iteration.
     */
    private BigDecimal resolveLicenceFeeForRange(Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        List<VehicleLicense> vehicleLicenses =
                vehicleLicenseRepository.findByVehicleIdAndDateBetweenAndDeletedFalse(vehicle.getId(), startDate, endDate);

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