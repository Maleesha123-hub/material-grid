//package com.pixelMind.materialGrid.service;
//
//import com.pixelMind.materialGrid.dto.response.DailyRouteReportResponse;
//
//import java.time.LocalDate;
//
//public interface DailyRouteReportService {
//
//    /**
//     * Read-only. Validates the vehicle exists, resolves the single Daily
//     * Route for (vehicleId, date), aggregates Vehicle Expenses, resolves the
//     * Licence Fee via VehicleLicense status (no License date-range check -
//     * see class Javadoc on the impl), and computes the balance.
//     */
//    DailyRouteReportResponse generateReport(LocalDate date, Long vehicleId);
//}