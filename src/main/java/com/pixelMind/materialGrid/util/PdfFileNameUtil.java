package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.constant.ReportConstants;

import java.time.LocalDate;

/**
 * MODIFIED: buildFileName now takes a date RANGE instead of a single date,
 * matching the report's new startDate/endDate parameters. Filename dates
 * use ISO (LocalDate.toString(), e.g. "2026-08-01") rather than the PDF's
 * on-page dd/MM/yyyy display format - filenames should stay
 * machine-sortable/parseable, which dd/MM/yyyy is not (see spec section 28:
 * ISO for API/machine contexts, dd/MM/yyyy only for human-facing PDF
 * display).
 */
public final class PdfFileNameUtil {

    private PdfFileNameUtil() {
    }

    public static String buildFileName(String vehicleNumber, LocalDate startDate, LocalDate endDate) {
        String safeVehicleNumber = vehicleNumber == null
                ? "unknown"
                : vehicleNumber.replaceAll("[^A-Za-z0-9-]", "");
        return ReportConstants.PDF_FILENAME_PREFIX + "-" + safeVehicleNumber
                + "-" + startDate + "-to-" + endDate + ".pdf";
    }
}