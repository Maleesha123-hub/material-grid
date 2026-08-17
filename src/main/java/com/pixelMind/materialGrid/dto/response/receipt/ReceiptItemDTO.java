package com.pixelMind.materialGrid.dto.response.receipt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptItemDTO {
    private int no;
    private String lorryNo;
    private String routeCode;
    private String billNumber;
    private double cube;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private BigDecimal dailyExpenses;
    private BigDecimal licenseFee;
    private BigDecimal balance;
}
