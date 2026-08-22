package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.response.DailyRouteReportResponse;
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
import com.pixelMind.materialGrid.service.impl.DailyRouteReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRouteReportServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DailyRouteRepository dailyRouteRepository;
    @Mock
    private VehicleExpenseRepository vehicleExpenseRepository;
    @Mock
    private VehicleLicenseRepository vehicleLicenseRepository;
    @Mock
    private LicenseRepository licenseRepository;

    @InjectMocks
    private DailyRouteReportServiceImpl reportService;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    private Vehicle vehicle() {
        return Vehicle.builder().id(1L).vehicleNumber("WP-CAB-1234").capacity(new BigDecimal("3.5")).build();
    }

    private DailyRoute dailyRoute(Vehicle vehicle) {
        return DailyRoute.builder().id(10L).date(DATE).vehicle(vehicle)
                .amount(new BigDecimal("10000.0000")).deleted(false).build();
    }

    @Test
    void vehicleNotFound_throwsResourceNotFoundException() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.generateReport(DATE, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void dailyRouteNotFound_throwsResourceNotFoundException() {
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of());

        assertThatThrownBy(() -> reportService.generateReport(DATE, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void duplicateDailyRoutes_throwsDataIntegrityBusinessException() {
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE))
                .thenReturn(List.of(dailyRoute(vehicle), dailyRoute(vehicle)));

        assertThatThrownBy(() -> reportService.generateReport(DATE, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCodeConstants.DATA_INTEGRITY_ERROR);
    }

    @Test
    void noExpenses_paidAmountIsZero() {
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of(dailyRoute(vehicle)));
        when(vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(1L, DATE)).thenReturn(BigDecimal.ZERO);
        when(vehicleLicenseRepository.findByVehicleIdAndDate(1L, DATE)).thenReturn(List.of());

        DailyRouteReportResponse response = reportService.generateReport(DATE, 1L);

        assertThat(response.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void noVehicleLicense_licenceFeeIsZero_noException() {
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of(dailyRoute(vehicle)));
        when(vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(1L, DATE)).thenReturn(new BigDecimal("2000"));
        when(vehicleLicenseRepository.findByVehicleIdAndDate(1L, DATE)).thenReturn(List.of());

        DailyRouteReportResponse response = reportService.generateReport(DATE, 1L);

        assertThat(response.getLicenceFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("8000.0000"));
    }

    @Test
    void inactiveVehicleLicense_licenceFeeIsZero_licenseNeverQueried() {
        Vehicle vehicle = vehicle();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle)
                .license(License.builder().id(3L).build()).status(VehicleLicenseStatus.INACTIVE).date(DATE).build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of(dailyRoute(vehicle)));
        when(vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(1L, DATE)).thenReturn(BigDecimal.ZERO);
        when(vehicleLicenseRepository.findByVehicleIdAndDate(1L, DATE)).thenReturn(List.of(vl));

        DailyRouteReportResponse response = reportService.generateReport(DATE, 1L);

        assertThat(response.getLicenceFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void activeVehicleLicense_licenceFeeEqualsLicensePrice() {
        Vehicle vehicle = vehicle();
        License license = License.builder().id(3L).price(new BigDecimal("1500.0000")).build();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle)
                .license(license).status(VehicleLicenseStatus.ACTIVE).date(DATE).build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of(dailyRoute(vehicle)));
        when(vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(1L, DATE)).thenReturn(new BigDecimal("2000"));
        when(vehicleLicenseRepository.findByVehicleIdAndDate(1L, DATE)).thenReturn(List.of(vl));
        when(licenseRepository.findById(3L)).thenReturn(Optional.of(license));

        DailyRouteReportResponse response = reportService.generateReport(DATE, 1L);

        assertThat(response.getLicenceFee()).isEqualByComparingTo(new BigDecimal("1500.0000"));
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("6500.0000")); // 10000 - (2000+1500)
    }

    @Test
    void activeVehicleLicense_licenseMissing_throwsDataIntegrityBusinessException() {
        Vehicle vehicle = vehicle();
        License phantomLicense = License.builder().id(999L).build();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle)
                .license(phantomLicense).status(VehicleLicenseStatus.ACTIVE).date(DATE).build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of(dailyRoute(vehicle)));
        when(vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(1L, DATE)).thenReturn(BigDecimal.ZERO);
        when(vehicleLicenseRepository.findByVehicleIdAndDate(1L, DATE)).thenReturn(List.of(vl));
        when(licenseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.generateReport(DATE, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCodeConstants.DATA_INTEGRITY_ERROR);
    }

    @Test
    void duplicateVehicleLicenses_throwsDataIntegrityBusinessException() {
        Vehicle vehicle = vehicle();
        VehicleLicense vl1 = VehicleLicense.builder().id(5L).vehicle(vehicle)
                .license(License.builder().id(3L).build()).status(VehicleLicenseStatus.ACTIVE).date(DATE).build();
        VehicleLicense vl2 = VehicleLicense.builder().id(6L).vehicle(vehicle)
                .license(License.builder().id(4L).build()).status(VehicleLicenseStatus.ACTIVE).date(DATE).build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dailyRouteRepository.findByVehicleIdAndDateAndDeletedFalse(1L, DATE)).thenReturn(List.of(dailyRoute(vehicle)));
        when(vehicleExpenseRepository.sumExpensesByVehicleIdAndDate(1L, DATE)).thenReturn(BigDecimal.ZERO);
        when(vehicleLicenseRepository.findByVehicleIdAndDate(1L, DATE)).thenReturn(List.of(vl1, vl2));

        assertThatThrownBy(() -> reportService.generateReport(DATE, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCodeConstants.DATA_INTEGRITY_ERROR);
    }
}