package com.pixelMind.materialGrid.constant;

public final class ReportConstants {

    private ReportConstants() {
    }

    // Centralized so branding/format changes are one-line edits, not
    // scattered magic strings inside the PDF rendering code.
    public static final String COMPANY_NAME = "MALSHI SUPPLIERS";
    public static final String COMPANY_TAGLINE = "BUILDING MATERIALS SUPPLIERS & TRANSPORT SERVICES";

    public static final String REPORT_DATE_PATTERN = "yyyy.MM.dd"; // matches the provided sample exactly
    public static final String CURRENCY_PREFIX = "Rs. ";

    public static final String PDF_FILENAME_PREFIX = "receipt";
}