package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLicenseUpdateRequest {

    @NotNull(message = "Vehicle id is required")
    private Long vehicleId;

    @NotNull(message = "License id is required")
    private Long licenseId;

    @NotNull(message = "Assigned date is required")
    private LocalDate assignedDate;

}
