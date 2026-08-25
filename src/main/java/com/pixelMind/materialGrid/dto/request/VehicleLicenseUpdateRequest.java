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
public class VehicleLicenseUpdateRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Status is required")
    private VehicleLicenseStatus status;

}
