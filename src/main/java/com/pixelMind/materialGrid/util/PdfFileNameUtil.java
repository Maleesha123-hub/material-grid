package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.constant.ReportConstants;

import java.time.LocalDate;

/**
 * Builds and sanitizes the downloadable PDF filename - vehicle numbers are
 * already constrained to [A-Z0-9-] at creation time (see Vehicle module),
 * but this sanitizes defensively regardless, since a filename derived from
 * data should never trust that upstream validation was never bypassed or
 * changed later.
 */
public final class PdfFileNameUtil {

    private PdfFileNameUtil() {
    }

    public static String buildFileName(String vehicleNumber, LocalDate date) {
        String safeVehicleNumber = vehicleNumber == null
                ? "unknown"
                : vehicleNumber.replaceAll("[^A-Za-z0-9-]", "");
        return ReportConstants.PDF_FILENAME_PREFIX + "-" + safeVehicleNumber + "-" + date + ".pdf";
    }
}