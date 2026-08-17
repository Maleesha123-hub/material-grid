package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.DailyRouteReportResponse;

public interface DailyRoutePdfService {

    /**
     * Pure rendering - accepts an already-built report DTO and returns PDF
     * bytes. Performs no database access.
     */
    byte[] generatePdf(DailyRouteReportResponse report);
}