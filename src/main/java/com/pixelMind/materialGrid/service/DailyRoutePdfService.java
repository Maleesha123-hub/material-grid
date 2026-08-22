package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceipt;

public interface DailyRoutePdfService {

    /**
     * Pure rendering - accepts an already-built payment receipt DTO and
     * returns PDF bytes. Performs no database access.
     */
    byte[] generatePdf(DailyRoutePaymentReceipt receipt);
}