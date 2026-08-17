package com.pixelMind.materialGrid.dto.response.receipt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptSummaryDTO {
    private LocalDate date;
    private String vehicleNumber;
    private String projectSite;
    private List<ReceiptItemDTO> items;
    private double totalCubes;
    private BigDecimal totalAmount;
    private BigDecimal totalExpenses;
    private BigDecimal totalLicenseFee;
    private BigDecimal totalBalance;
}
