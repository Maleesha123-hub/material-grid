package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
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
import com.pixelMind.materialGrid.service.impl.DailyRouteImportServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyRouteImportServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private VehicleLicenseRepository vehicleLicenseRepository;
    @Mock
    private DailyRouteRepository dailyRouteRepository;
    @Mock
    private PriceRateRepository priceRateRepository;

    @InjectMocks
    private DailyRouteImportServiceImpl importService;

    private MockMultipartFile buildExcel(Object[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Vehicle Number");
            header.createCell(2).setCellValue("Route Code");
            header.createCell(3).setCellValue("Check By");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue((String) rows[r][c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "routes.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private Vehicle vehicle() {
        return Vehicle.builder().id(1L).vehicleNumber("WP-CAB-1234").build();
    }

    private Route route() {
        return Route.builder().id(2L).routeCode("RT000001").km(new BigDecimal("10.00")).build();
    }

    private License license() {
        return License.builder().id(3L).licenseCode("LIC000001")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 12, 31)).build();
    }

    @Test
    void validRow_inactiveVehicleLicense_activatesAndCreatesDailyRoute() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();
        License license = license();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle).license(license)
                .status(VehicleLicenseStatus.INACTIVE).date(LocalDate.of(2026, 1, 1)).build();
        PriceRate activeRate = PriceRate.builder().id(9L).price(new BigDecimal("150.0000")).status(PriceRateStatus.ACTIVE).build();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of(license));
        when(vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(anyCollection(), anyCollection())).thenReturn(List.of(vl));
        when(dailyRouteRepository.findPotentialDuplicates(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of());
        when(priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)).thenReturn(java.util.Optional.of(activeRate));
        when(vehicleLicenseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dailyRouteRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Shehan"});

        BulkUploadResponse response = importService.importFromExcel(file);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(vl.getStatus()).isEqualTo(VehicleLicenseStatus.ACTIVE);
        assertThat(vl.getDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        verify(vehicleLicenseRepository).saveAll(any());
        verify(dailyRouteRepository).saveAll(any());
    }

    @Test
    void noActiveLicenseForDate_isRejected_nothingInserted() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of());

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Shehan"});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> assertThat(((ExcelValidationException) ex).getErrors())
                        .anyMatch(e -> e.getMessage().contains("No valid license found")));

        verify(dailyRouteRepository, never()).saveAll(any());
        verify(vehicleLicenseRepository, never()).saveAll(any());
    }

    @Test
    void vehicleLicenseNotFound_isRejected() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();
        License license = license();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of(license));
        when(vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(anyCollection(), anyCollection())).thenReturn(List.of());

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Shehan"});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> assertThat(((ExcelValidationException) ex).getErrors())
                        .anyMatch(e -> e.getMessage().contains("No vehicle license found")));

        verify(dailyRouteRepository, never()).saveAll(any());
    }

    @Test
    void noActivePriceRate_throwsBusinessException_afterValidationPasses() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();
        License license = license();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle).license(license)
                .status(VehicleLicenseStatus.ACTIVE).date(LocalDate.of(2026, 1, 1)).build();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of(license));
        when(vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(anyCollection(), anyCollection())).thenReturn(List.of(vl));
        when(dailyRouteRepository.findPotentialDuplicates(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of());
        when(priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)).thenReturn(java.util.Optional.empty());

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Shehan"});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCodeConstants.ACTIVE_PRICE_RATE_NOT_FOUND);

        verify(dailyRouteRepository, never()).saveAll(any());
        // VehicleLicense was ACTIVE already, so no activation was attempted anyway,
        // but critically nothing was persisted at all for this failed import.
        verify(vehicleLicenseRepository, never()).saveAll(any());
    }

    @Test
    void nineValidRowsPlusOneInvalid_insertsNothing_andActivatesNoVehicleLicenses() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();
        License license = license();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle).license(license)
                .status(VehicleLicenseStatus.INACTIVE).date(LocalDate.of(2026, 1, 1)).build();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of(license));
        when(vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(anyCollection(), anyCollection())).thenReturn(List.of(vl));
        when(dailyRouteRepository.findPotentialDuplicates(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of());

        // 9 distinct valid rows (different dates keep them out of the duplicate check) + 1 row with an unknown route.
        Object[][] rows = new Object[10][];
        for (int i = 0; i < 9; i++) {
            rows[i] = new Object[]{"2026-08-0" + (i + 1), "WP-CAB-1234", "RT000001", "Shehan"};
        }
        rows[9] = new Object[]{"2026-08-10", "WP-CAB-1234", "RT999999", "Shehan"}; // unknown route code

        MockMultipartFile file = buildExcel(rows);

        assertThatThrownBy(() -> importService.importFromExcel(file)).isInstanceOf(ExcelValidationException.class);

        assertThat(vl.getStatus()).isEqualTo(VehicleLicenseStatus.INACTIVE); // untouched
        verify(dailyRouteRepository, never()).saveAll(any());
        verify(vehicleLicenseRepository, never()).saveAll(any());
        verify(priceRateRepository, never()).findByStatus(any());
    }

    @Test
    void duplicateRowsWithinFile_areRejected() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();
        License license = license();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle).license(license)
                .status(VehicleLicenseStatus.ACTIVE).date(LocalDate.of(2026, 1, 1)).build();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of(license));
        when(vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(anyCollection(), anyCollection())).thenReturn(List.of(vl));

        MockMultipartFile file = buildExcel(
                new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Shehan"},
                new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Nimal"});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> assertThat(((ExcelValidationException) ex).getErrors())
                        .anyMatch(e -> e.getMessage().contains("Duplicate")));

        verify(dailyRouteRepository, never()).saveAll(any());
    }

    @Test
    void duplicateAgainstExistingHistory_isRejected() throws IOException {
        Vehicle vehicle = vehicle();
        Route route = route();
        License license = license();
        VehicleLicense vl = VehicleLicense.builder().id(5L).vehicle(vehicle).license(license)
                .status(VehicleLicenseStatus.ACTIVE).date(LocalDate.of(2026, 1, 1)).build();
        DailyRoute existing = DailyRoute.builder().id(99L).date(LocalDate.of(2026, 8, 1)).vehicle(vehicle).route(route).build();

        when(vehicleRepository.findByVehicleNumberIn(anyCollection())).thenReturn(List.of(vehicle));
        when(routeRepository.findByRouteCodeIn(anyCollection())).thenReturn(List.of(route));
        when(licenseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of(license));
        when(vehicleLicenseRepository.findByVehicleIdInAndLicenseIdIn(anyCollection(), anyCollection())).thenReturn(List.of(vl));
        when(dailyRouteRepository.findPotentialDuplicates(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of(existing));

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-1234", "RT000001", "Shehan"});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> assertThat(((ExcelValidationException) ex).getErrors())
                        .anyMatch(e -> e.getMessage().contains("already exists")));

        verify(dailyRouteRepository, never()).saveAll(any());
    }
}