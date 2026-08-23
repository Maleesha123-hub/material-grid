/*
package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.service.impl.DailyRoutePdfServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DailyRoutePdfServiceImplTest {

    private final DailyRoutePdfServiceImpl pdfService = new DailyRoutePdfServiceImpl();

    @Test
    void generatePdf_producesNonEmptyValidPdfBytes() {
        DailyRouteReportResponse report = DailyRouteReportResponse.builder()
                .date(LocalDate.of(2026, 8, 17))
                .vehicleNumber("WP-CAB-1234")
                .vehicleCapacity(new BigDecimal("3.5"))
                .loadCount(11)
                .totalAmount(new BigDecimal("10000.0000"))
                .paidAmount(new BigDecimal("2000.0000"))
                .licenceFee(new BigDecimal("1500.0000"))
                .balance(new BigDecimal("6500.0000"))
                .build();

        byte[] pdfBytes = pdfService.generatePdf(report);

        assertThat(pdfBytes).isNotEmpty();
        // Every valid PDF file starts with the "%PDF-" magic bytes.
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}*/
