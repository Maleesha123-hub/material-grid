package com.pixelMind.materialGrid.dto.request;

import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLicenseCreateRequest {

    @NotNull(message = "Vehicle id is required")
    private Long vehicleId;

    @NotNull(message = "License id is required")
    private Long licenseId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Status is required")
    private VehicleLicenseStatus status;
}
