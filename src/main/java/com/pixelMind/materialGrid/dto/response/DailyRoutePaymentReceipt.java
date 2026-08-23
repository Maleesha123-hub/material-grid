package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DailyRoutePaymentReceipt {
    private String vehicleNumber;
    private BigDecimal vehicleCapacity;
    private LocalDate startDate;
    private LocalDate endDate;

    private List<DailyRoutePaymentReceiptRow> rows;

    private Integer totalLoadCount;
    /** ADDED: sum of every row's totalKm across the whole date range. */
    private BigDecimal totalKm;
    private BigDecimal priceRate;
    private boolean priceRateVaries;
    private BigDecimal totalAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal licenceFee;
    private BigDecimal balance;
}