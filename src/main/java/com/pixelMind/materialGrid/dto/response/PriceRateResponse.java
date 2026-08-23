package com.pixelMind.materialGrid.dto.response;

import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PriceRateResponse {
    private Long id;
    private BigDecimal price;
    private PriceRateStatus status;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}
