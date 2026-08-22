package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.DuplicateFileUploadException;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.impl.VehicleImportServiceImpl;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleImportServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private FileHistoryService fileHistoryService;

    @InjectMocks
    private VehicleImportServiceImpl importService;

    private MockMultipartFile buildExcel(String fileName, Object[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Vehicle Number");
            header.createCell(1).setCellValue("Capacity");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue((String) rows[r][0]);
                if (rows[r][1] instanceof Double d) row.createCell(1).setCellValue(d);
                else row.createCell(1).setCellValue((String) rows[r][1]);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void duplicateFile_isRejectedBeforeParsing_noFileHistoryOrVehiclesCreated() throws IOException {
        doThrow(new DuplicateFileUploadException("File 'vehicles.xlsx' has already been uploaded as VEHICLE.", "DUPLICATE_FILE_UPLOAD"))
                .when(fileHistoryService).validateNotAlreadyUploaded("vehicles.xlsx", FileType.VEHICLE);

        MockMultipartFile file = buildExcel("vehicles.xlsx", new Object[]{"WP-CAB-1234", 15.0});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(DuplicateFileUploadException.class);

        verify(fileHistoryService, never()).createFileHistory(any(), any());
        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void allRowsValid_createsFileHistoryAndVehicles_taggedWithFileHistory() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());
        FileHistory fileHistory = FileHistory.builder().id(7L).fileName("vehicles.xlsx").fileType(FileType.VEHICLE).build();
        when(fileHistoryService.createFileHistory("vehicles.xlsx", FileType.VEHICLE)).thenReturn(fileHistory);
        when(vehicleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildExcel("vehicles.xlsx",
                new Object[]{"WP-CAB-1234", 15.0},
                new Object[]{"WP-CAB-5678", 20.0});

        BulkUploadResponse response = importService.importFromExcel(file);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFileHistoryId()).isEqualTo(7L);
        assertThat(response.getFileType()).isEqualTo("VEHICLE");

        verify(vehicleRepository).saveAll(argThat((List<Vehicle> list) ->
                list.size() == 2 && list.stream().allMatch(v -> v.getFileHistory() == fileHistory)));
    }

    @Test
    void duplicateVehicleNumberWithinFile_rejectsWholeFile_noFileHistoryCreated() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());

        MockMultipartFile file = buildExcel("vehicles.xlsx",
                new Object[]{"WP-CAB-1234", 15.0},
                new Object[]{"WP-CAB-1234", 20.0});

        assertThatThrownBy(() -> importService.importFromExcel(file)).isInstanceOf(ExcelValidationException.class);

        verify(fileHistoryService, never()).createFileHistory(any(), any());
        verify(vehicleRepository, never()).saveAll(any());
    }

    @Test
    void vehicleNumberAlreadyInDatabase_rejectsWholeFile() throws IOException {
        Vehicle existing = Vehicle.builder().id(1L).vehicleNumber("WP-CAB-1234").build();
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of(existing));

        MockMultipartFile file = buildExcel("vehicles.xlsx", new Object[]{"WP-CAB-1234", 15.0});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(ExcelValidationException.class)
                .satisfies(ex -> assertThat(((ExcelValidationException) ex).getErrors())
                        .anyMatch(e -> e.getMessage().contains("already exists")));

        verify(fileHistoryService, never()).createFileHistory(any(), any());
    }

    @Test
    void nineValidRowsPlusOneInvalid_insertsNothing_createsNoFileHistory() throws IOException {
        when(vehicleRepository.findByVehicleNumberIn(anySet())).thenReturn(List.of());

        Object[][] rows = new Object[10][];
        for (int i = 0; i < 9; i++) {
            rows[i] = new Object[]{"WP-CAB-100" + i, 15.0};
        }
        rows[9] = new Object[]{"BAD", -5.0}; // invalid: too-short number AND negative capacity

        MockMultipartFile file = buildExcel("vehicles.xlsx", rows);

        assertThatThrownBy(() -> importService.importFromExcel(file)).isInstanceOf(ExcelValidationException.class);

        verify(fileHistoryService, never()).createFileHistory(any(), any());
        verify(vehicleRepository, never()).saveAll(any());
    }
}