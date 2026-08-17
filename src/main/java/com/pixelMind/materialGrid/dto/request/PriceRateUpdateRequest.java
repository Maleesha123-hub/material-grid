package com.pixelMind.materialGrid.dto.request;

import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PriceRateUpdateRequest {

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0001", inclusive = true, message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Status is required")
    private PriceRateStatus status;
}
