package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateRequest {

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.01", message = "Capacity must be greater than zero")
    private BigDecimal capacity;

    // vehicleNumber is treated as immutable after creation, same rationale
    // as routeCode/licenseCode: it is how the vehicle is referenced
    // everywhere else (expenses, licenses, daily routes) so changing it
    // silently would be surprising. Expose a dedicated endpoint later if a
    // genuine re-plating business need arises.
}
