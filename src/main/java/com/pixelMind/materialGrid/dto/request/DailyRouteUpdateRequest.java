package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyRouteUpdateRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Vehicle id is required")
    private Long vehicleId;

//    @NotNull(message = "Route id is required")
    private Long routeId;

    @NotBlank(message = "Bil Number is required")
    @Size(max = 100)
    private String billNumber;
}