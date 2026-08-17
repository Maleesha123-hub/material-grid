package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequest {

    @NotBlank(message = "Vehicle number is required")
    @Pattern(regexp = "^[A-Z0-9-]{4,20}$", message = "Vehicle number must be 4-20 uppercase letters, digits, or hyphens")
    private String vehicleNumber;

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.01", message = "Capacity must be greater than zero")
    private BigDecimal capacity;
}
