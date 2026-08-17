package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelUtilTest {

    private MockMultipartFile buildXlsx(String[] headers, Object[]... rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = rows[r];
                for (int c = 0; c < values.length; c++) {
                    Object v = values[c];
                    if (v instanceof String s) row.createCell(c).setCellValue(s);
                    else if (v instanceof Double d) row.createCell(c).setCellValue(d);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void openWorkbook_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);
        assertThatThrownBy(() -> ExcelUtil.openWorkbook(empty)).isInstanceOf(ExcelValidationException.class);
    }

    @Test
    void openWorkbook_unsupportedExtension_throws() {
        MockMultipartFile txt = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        assertThatThrownBy(() -> ExcelUtil.openWorkbook(txt)).isInstanceOf(ExcelValidationException.class);
    }

    @Test
    void requireHeaders_missingColumn_throws() throws IOException {
        MockMultipartFile file = buildXlsx(new String[]{"Date", "Vehicle", "Amount"}, new Object[]{"2026-08-01", "WP-1234", 100.0});
        try (Workbook wb = ExcelUtil.openWorkbook(file)) {
            Sheet sheet = ExcelUtil.firstSheet(wb);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            assertThatThrownBy(() -> ExcelUtil.requireHeaders(headerIndex, List.of("Date", "Vehicle Number", "Expense")))
                    .isInstanceOf(ExcelValidationException.class);
        }
    }

    @Test
    void requireHeaders_caseAndSpaceInsensitive_passes() throws IOException {
        MockMultipartFile file = buildXlsx(new String[]{" date ", "VEHICLE NUMBER", "expense"}, new Object[]{"2026-08-01", "WP-1234", 100.0});
        try (Workbook wb = ExcelUtil.openWorkbook(file)) {
            Sheet sheet = ExcelUtil.firstSheet(wb);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, List.of("Date", "Vehicle Number", "Expense")); // no throw
            assertThat(headerIndex).containsKeys("date", "vehicle number", "expense");
        }
    }

    @Test
    void readDate_invalidText_returnsEmpty() throws IOException {
        MockMultipartFile file = buildXlsx(new String[]{"Date"}, new Object[]{"abc"});
        try (Workbook wb = ExcelUtil.openWorkbook(file)) {
            Sheet sheet = ExcelUtil.firstSheet(wb);
            assertThat(ExcelUtil.readDate(sheet.getRow(1), 0)).isEmpty();
        }
    }

    @Test
    void readDate_isoString_parses() throws IOException {
        MockMultipartFile file = buildXlsx(new String[]{"Date"}, new Object[]{"2026-08-16"});
        try (Workbook wb = ExcelUtil.openWorkbook(file)) {
            Sheet sheet = ExcelUtil.firstSheet(wb);
            assertThat(ExcelUtil.readDate(sheet.getRow(1), 0)).contains(LocalDate.of(2026, 8, 16));
        }
    }

    @Test
    void readBigDecimal_numericCell_parses() throws IOException {
        MockMultipartFile file = buildXlsx(new String[]{"Expense"}, new Object[]{2500.50});
        try (Workbook wb = ExcelUtil.openWorkbook(file)) {
            Sheet sheet = ExcelUtil.firstSheet(wb);
            assertThat(ExcelUtil.readBigDecimal(sheet.getRow(1), 0)).contains(BigDecimal.valueOf(2500.50));
        }
    }

    @Test
    void isRowEmpty_blankRow_true() throws IOException {
        MockMultipartFile file = buildXlsx(new String[]{"A", "B"}, new Object[]{"", ""});
        try (Workbook wb = ExcelUtil.openWorkbook(file)) {
            Sheet sheet = ExcelUtil.firstSheet(wb);
            assertThat(ExcelUtil.isRowEmpty(sheet.getRow(1))).isTrue();
        }
    }
}