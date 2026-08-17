package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleExpenseUpdateRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Expenses is required")
    @DecimalMin(value = "0.0001", message = "Expenses must be greater than zero")
    private BigDecimal expenses;

    // vehicle association is immutable on update, same historical-integrity
    // rationale as VehicleLicense.
}
