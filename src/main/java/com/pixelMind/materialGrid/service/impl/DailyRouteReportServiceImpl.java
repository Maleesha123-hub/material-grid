package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.response.DailyRouteReportResponse;
import com.pixelMind.materialGrid.dto.response.ReceiptSummaryDTO;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.entity.Vehicle;
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
import java.util.List;

/**
 * MANDATORY rule (see spec sections 13/19/47): the Licence Fee lookup NEVER
 * checks License.startDate/endDate. The applicable License for a given
 * (vehicleId, date) is determined solely by the VehicleLicense row for that
 * exact pair, per its status - not by any date-range search.
 *
 * Every method here is read-only: generating a report must never create,
 * update, or delete any row.
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
    public DailyRouteReportResponse generateReport(LocalDate date, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle with ID " + vehicleId + " does not exist", ErrorCodeConstants.VEHICLE_NOT_FOUND));

        List<DailyRoute> dailyRoutes = dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(vehicle.getId(), date);

        if (dailyRoutes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No daily route found for vehicle " + vehicle.getVehicleNumber() + " on " + date,
                    ErrorCodeConstants.DAILY_ROUTE_NOT_FOUND);
        }

        BigDecimal paidAmount = vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(vehicleId, date);
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        BigDecimal licenceFee = resolveLicenceFee(vehicle, date);

        BigDecimal totalAmount = dailyRoutes.stream()
                .map(DailyRoute::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalAmount.subtract(paidAmount.add(licenceFee));

        log.info("Daily route report generated: vehicleId={}, date={}, totalAmount={}, paidAmount={}, "
                        + "licenceFee={}, balance={}",
                vehicleId, date, totalAmount, paidAmount, licenceFee, balance);

        return DailyRouteReportResponse.builder()
                .date(date)
                .vehicleNumber(vehicle.getVehicleNumber())
                .vehicleCapacity(vehicle.getCapacity())
                .loadCount(dailyRoutes.size())
                .totalVolume(vehicle.getCapacity().multiply(BigDecimal.valueOf(dailyRoutes.size())).doubleValue())
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .licenceFee(licenceFee)
                .balance(balance)
                .build();
    }

    @Override
    public ReceiptSummaryDTO getSummary(LocalDate date, Long vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle with ID " + vehicleId + " does not exist", ErrorCodeConstants.VEHICLE_NOT_FOUND)
                );

        // Daily routes
        Integer loadCount = dailyRouteRepository.loadCountByVehicleIdAndDate(
                vehicleId, date
        );

        // Daily expenses
        BigDecimal dailyExpenses = vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(
                vehicleId, date
        );

        // License amount
        BigDecimal licenseAmount = vehicleLicenseRepository.sumLicenseAmountByVehicleIdAndDate(
                vehicleId, date, VehicleLicenseStatus.ACTIVE
        );

        // Gross transport rate
        BigDecimal dailyGrossAmount = dailyRouteRepository.sumAmountsByVehicleIdAndDate(vehicleId, date);

        return ReceiptSummaryDTO.builder()
                .totalDispatches(loadCount)
                .totalVolumes(vehicle.getCapacity().multiply(new BigDecimal(loadCount)).doubleValue())
                .dailyGrossTransportRate(dailyGrossAmount)
                .dailyDeduction(dailyExpenses.add(licenseAmount))
                .payable(dailyGrossAmount.subtract(dailyExpenses.add(licenseAmount)))
                .build();

    }

    private DailyRoute resolveSingleDailyRoute(Vehicle vehicle, LocalDate date) {
        List<DailyRoute> dailyRoutes = dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(vehicle.getId(), date);

        if (dailyRoutes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No daily route found for vehicle " + vehicle.getVehicleNumber() + " on " + date,
                    ErrorCodeConstants.DAILY_ROUTE_NOT_FOUND);
        }
        if (dailyRoutes.size() > 1) {
            log.error("Data integrity error: {} daily route records found for vehicleId={}, date={}",
                    dailyRoutes.size(), vehicle.getId(), date);
            throw new BusinessException(
                    "Multiple daily route records exist for vehicle " + vehicle.getVehicleNumber()
                            + " on " + date + ". This indicates a data integrity issue.",
                    ErrorCodeConstants.DATA_INTEGRITY_ERROR);
        }
        return dailyRoutes.get(0);
    }

    private BigDecimal resolveLicenceFee(Vehicle vehicle, LocalDate date) {
        List<VehicleLicense> vehicleLicenses = vehicleLicenseRepository.findByVehicleIdAndDate(vehicle.getId(), date);

        if (vehicleLicenses.isEmpty()) {
            return BigDecimal.ZERO; // per spec section 15 - not an error
        }
        if (vehicleLicenses.size() > 1) {
            log.error("Data integrity error: {} vehicle license records found for vehicleId={}, date={}",
                    vehicleLicenses.size(), vehicle.getId(), date);
            throw new BusinessException(
                    "Multiple vehicle license records exist for vehicle " + vehicle.getVehicleNumber()
                            + " on " + date + ". This indicates a data integrity issue.",
                    ErrorCodeConstants.DATA_INTEGRITY_ERROR);
        }

        VehicleLicense vehicleLicense = vehicleLicenses.getFirst();
        if (vehicleLicense.getStatus() != VehicleLicenseStatus.ACTIVE) {
            return BigDecimal.ZERO; // INACTIVE - per spec section 17
        }

        Long licenseId = vehicleLicense.getLicense().getId();
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> {
                    log.error("Data integrity error: VehicleLicense id={} is ACTIVE but references "
                            + "nonexistent License id={}", vehicleLicense.getId(), licenseId);
                    return new BusinessException(
                            "The active vehicle license for " + vehicle.getVehicleNumber()
                                    + " references a license that no longer exists. This indicates a data integrity issue.",
                            ErrorCodeConstants.DATA_INTEGRITY_ERROR);
                });

        return license.getPrice();
    }
}