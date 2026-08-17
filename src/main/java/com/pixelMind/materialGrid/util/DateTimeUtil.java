package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.constant.ReportConstants;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    private static final Clock CLOCK = Clock.system(ZoneOffset.UTC);

    private static final DateTimeFormatter REPORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(ReportConstants.REPORT_DATE_PATTERN);

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(CLOCK);
    }

    /**
     * Formats a date exactly as shown in the provided sample PDF ("2026.08.11").
     */
    public static String formatReportDate(LocalDate date) {
        return date == null ? "" : date.format(REPORT_DATE_FORMATTER);
    }
}