package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DailyRouteResponse {
    private Long id;
    private LocalDate date;
    private VehicleSummaryResponse vehicle;
    private RouteSummaryResponse route;
    private PriceRateSummaryResponse priceRate;
    private BigDecimal amount;
    private String checkBy;
    private String bilNumber;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}