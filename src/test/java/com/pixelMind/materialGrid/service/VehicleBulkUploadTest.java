/*
package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.impl.VehicleServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleBulkUploadTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleExpenseRepository vehicleExpenseRepository;
    @Mock
    private VehicleLicenseRepository vehicleLicenseRepository;
    @Mock
    private DailyRouteRepository dailyRouteRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private MockMultipartFile buildExcel(String col1Header, String col2Header, Object[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(col1Header);
            header.createCell(1).setCellValue(col2Header);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue((String) rows[r][0]);
                if (rows[r][1] instanceof Number n) {
                    row.createCell(1).setCellValue(n.doubleValue());
                } else if (rows[r][1] != null) {
                    row.createCell(1).setCellValue(rows[r][1].toString());
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "vehicles.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void allRowsValid_numericCapacity_insertsAll() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());
        when(vehicleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"WP-CAB-1234", 5},
                new Object[]{"WP-CAB-5678", 5.3});

        BulkUploadResponse response = vehicleService.bulkUploadVehicles(file);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalRows()).isEqualTo(2);
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getErrorCount()).isEqualTo(0);
        assertThat(response.getErrors()).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Vehicle>> captor = ArgumentCaptor.forClass(List.class);
        verify(vehicleRepository).saveAll(captor.capture());

        List<Vehicle> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getVehicleNumber()).isEqualTo("WP-CAB-1234");
        assertThat(saved.get(0).getCapacity()).isEqualByComparingTo(new BigDecimal("5"));

        assertThat(saved.get(1).getVehicleNumber()).isEqualTo("WP-CAB-5678");
        assertThat(saved.get(1).getCapacity()).isEqualByComparingTo(new BigDecimal("5.3"));
    }

    @Test
    void flexibleHeaders_validRows_allInsertedSuccessfully() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());
        when(vehicleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity",
                new Object[]{"WP-CAB-9999", 7.5});

        BulkUploadResponse response = vehicleService.bulkUploadVehicles(file);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSuccessCount()).isEqualTo(1);
    }

    @Test
    void stringCapacity_rejectedWithValidationException() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());

        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"WP-CAB-1234", "five_cube"});

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).anyMatch(e ->
                            e.getField().equals("Capacity") && e.getMessage().contains("must be a valid number"));
                });

        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void negativeOrZeroCapacity_rejected() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());

        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"WP-CAB-1234", 0},
                new Object[]{"WP-CAB-5678", -2.5});

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).hasSize(2);
                    assertThat(evex.getErrors()).allMatch(e ->
                            e.getField().equals("Capacity") && e.getMessage().contains("greater than zero"));
                });

        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void missingVehicleNumber_rejected() throws IOException {
        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"", 5.0});

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).anyMatch(e ->
                            e.getField().equals("Vehicle Number") && e.getMessage().contains("Vehicle number is required"));
                });

        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void invalidVehicleNumberFormat_rejected() throws IOException {
        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"AB", 5.0}); // less than 4 chars

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).anyMatch(e ->
                            e.getField().equals("Vehicle Number") && e.getMessage().contains("4-20 uppercase letters"));
                });

        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void duplicateVehicleNumberInFile_rejected() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());

        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"WP-CAB-1234", 5.0},
                new Object[]{"WP-CAB-1234", 6.0});

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).anyMatch(e ->
                            e.getMessage().contains("Duplicate vehicle number 'WP-CAB-1234' in uploaded file"));
                });

        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void duplicateVehicleNumberInDatabase_rejected() throws IOException {
        Vehicle existing = Vehicle.builder().id(10L).vehicleNumber("WP-CAB-1234").build();
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of(existing));

        MockMultipartFile file = buildExcel("Vehicle Number", "Capacity(cube)",
                new Object[]{"WP-CAB-1234", 5.0});

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).anyMatch(e ->
                            e.getMessage().contains("Vehicle number 'WP-CAB-1234' already exists"));
                });

        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void missingRequiredHeaders_rejected() throws IOException {
        MockMultipartFile file = buildExcel("Some Other Header", "Another Header",
                new Object[]{"Val1", "Val2"});

        assertThatThrownBy(() -> vehicleService.bulkUploadVehicles(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getMessage()).contains("Missing required column(s)");
                });

        verify(vehicleRepository, never()).saveAll(any());
    }
}
*/
