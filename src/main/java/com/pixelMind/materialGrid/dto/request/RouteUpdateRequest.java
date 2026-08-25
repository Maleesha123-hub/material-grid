package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RouteUpdateRequest {

    @NotBlank(message = "Start location is required")
    @Size(max = 150)
    private String startLocation;

    @NotBlank(message = "End location is required")
    @Size(max = 150)
    private String endLocation;

    @NotNull(message = "Km is required")
    @DecimalMin(value = "0.01", message = "Km must be greater than zero")
    private BigDecimal km;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0001", inclusive = true, message = "Price must be greater than zero")
    private BigDecimal price;

}
