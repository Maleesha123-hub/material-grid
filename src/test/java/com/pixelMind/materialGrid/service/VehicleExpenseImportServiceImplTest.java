/*
package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleExpense;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.impl.VehicleExpenseImportServiceImpl;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleExpenseImportServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleExpenseRepository vehicleExpenseRepository;

    @InjectMocks
    private VehicleExpenseImportServiceImpl importService;

    private MockMultipartFile buildExcel(Object[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Vehicle Number");
            header.createCell(2).setCellValue("Expense");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue((String) rows[r][0]);
                row.createCell(1).setCellValue((String) rows[r][1]);
                if (rows[r][2] instanceof Double d) row.createCell(2).setCellValue(d);
                else row.createCell(2).setCellValue((String) rows[r][2]);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "expenses.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void allRowsValid_insertsAll() throws IOException {
        Vehicle v1 = Vehicle.builder().id(1L).vehicleNumber("WP-CAB-1234").build();
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of(v1));
        when(vehicleExpenseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildExcel(
                new Object[]{"2026-08-01", "WP-CAB-1234", 2500.00},
                new Object[]{"2026-08-02", "WP-CAB-1234", 1800.00});

        BulkUploadResponse response = importService.importFromExcel(file);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getErrorCount()).isEqualTo(0);
    }

    @Test
    void oneInvalidRowAmongMany_insertsNothing() throws IOException {
        Vehicle v1 = Vehicle.builder().id(1L).vehicleNumber("WP-CAB-1234").build();
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of(v1));

        Object[][] rows = new Object[10][];
        for (int i = 0; i < 9; i++) {
            rows[i] = new Object[]{"2026-08-01", "WP-CAB-1234", 100.0 + i};
        }
        rows[9] = new Object[]{"2026-08-01", "WP-CAB-1234", "not-a-number"}; // row 10 invalid

        MockMultipartFile file = buildExcel(rows);

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class);

        verify(vehicleExpenseRepository, never()).saveAll(any());
        verify(vehicleExpenseRepository, never()).save(any());
    }

    @Test
    void unknownVehicleNumber_isRejected_nothingInserted() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-9999", 100.0});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> {
                    ExcelValidationException evex = (ExcelValidationException) ex;
                    assertThat(evex.getErrors()).anyMatch(e -> e.getMessage().contains("WP-CAB-9999"));
                });

        verify(vehicleExpenseRepository, never()).saveAll(any());
    }

    @Test
    void negativeExpense_isRejected() throws IOException {
        Vehicle v1 = Vehicle.builder().id(1L).vehicleNumber("WP-CAB-1234").build();
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of(v1));

        MockMultipartFile file = buildExcel(new Object[]{"2026-08-01", "WP-CAB-1234", -50.0});

        assertThatThrownBy(() -> importService.importFromExcel(file)).isInstanceOf(ExcelValidationException.class);
        verify(vehicleExpenseRepository, never()).saveAll(any());
    }
}*/
