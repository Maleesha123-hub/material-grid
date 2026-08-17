package com.pixelMind.materialGrid.dto.dailyRoutes;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DailyRoutesExcelDataDTO {

    private int rowNumber;
    private LocalDate date;
    private String routeCode;
    private String vehicleNumber;
    private String landCode;
    private String billNumber;
    private double cube;
    private double km;
    private BigDecimal dailyExpenses;
}
