package com.pixelMind.materialGrid.constant;

import java.util.List;
import java.util.Set;

public final class ExcelConstants {

    private ExcelConstants() {
    }

    public static final Set<String> ALLOWED_EXCEL_EXTENSIONS = Set.of("xlsx", "xls");
    public static final List<String> VEHICLE_HEADERS = List.of("Vehicle Number", "Capacity");
    public static final List<String> VEHICLE_EXPENSE_HEADERS = List.of("Date", "Vehicle Number", "Expense");
    public static final List<String> DAILY_ROUTE_HEADERS = List.of("Date", "Vehicle Number", "Route Code", "Check By");
    public static final List<String> VEHICLE_LICENSE_HEADERS = List.of("Date", "Vehicle Number", "License Code");
}