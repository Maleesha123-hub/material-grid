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
public class VehicleExpenseResponse {
    private Long id;
    private LocalDate date;
    private BigDecimal expenses;
    private BigDecimal amount;
    private Long vehicleId;
    private String vehicleNumber;
    private Long fileHistoryId;
    private FileHistoryResponse fileHistory;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}
