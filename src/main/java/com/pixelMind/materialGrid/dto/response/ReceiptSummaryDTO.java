package com.pixelMind.materialGrid.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
public class ReceiptSummaryDTO {

    private Integer totalDispatches;
    private double totalVolumes;
    private BigDecimal dailyGrossTransportRate;
    private BigDecimal dailyDeduction;
    private BigDecimal payable;
}
