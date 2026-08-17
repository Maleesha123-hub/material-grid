package com.pixelMind.materialGrid.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFileNameUtilTest {

    @Test
    void buildFileName_matchesExpectedFormat() {
        String fileName = PdfFileNameUtil.buildFileName("WP-CAB-1234", LocalDate.of(2026, 8, 17));
        assertThat(fileName).isEqualTo("daily-route-WP-CAB-1234-2026-08-17.pdf");
    }

    @Test
    void buildFileName_sanitizesUnsafeCharacters() {
        String fileName = PdfFileNameUtil.buildFileName("WP/../CAB 1234", LocalDate.of(2026, 8, 17));
        assertThat(fileName).isEqualTo("daily-route-WPCAB1234-2026-08-17.pdf");
    }
}