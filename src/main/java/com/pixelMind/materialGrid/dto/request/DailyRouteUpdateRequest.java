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

    @NotNull(message = "Route id is required")
    private Long routeId;

    @NotBlank(message = "Check By is required")
    @Size(max = 100)
    private String checkBy;

    // As with create, priceRateId is intentionally absent - amount is always
    // recalculated server-side against the CURRENT active PriceRate.
    // NOTE: this supersedes the prior iteration's "vehicle/route immutable
    // on update" decision, because this spec explicitly requires
    // re-validating Vehicle and Route on update. See architectural decision
    // #11 in the accompanying notes if you'd rather keep them immutable.
}