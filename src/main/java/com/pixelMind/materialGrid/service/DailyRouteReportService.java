package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceipt;
import com.pixelMind.materialGrid.dto.response.ReceiptSummaryDTO;

import java.time.LocalDate;

public interface DailyRouteReportService {

    /**
     * Read-only. Validates the vehicle exists, resolves the single Daily
     * Route for (vehicleId, date), aggregates Vehicle Expenses, resolves the
     * Licence Fee via VehicleLicense status (no License date-range check -
     * see class Javadoc on the impl), and computes the balance.
     */
    DailyRoutePaymentReceipt generateReport(LocalDate startDate, LocalDate endDate, Long vehicleId);
    ReceiptSummaryDTO getSummary(LocalDate date, Long vehicleId);
}