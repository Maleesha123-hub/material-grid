package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One CONSOLIDATED row of the vehicle payment receipt - one per distinct
 * date in the requested range, never one per raw DailyRoute record.
 *
 * `routeCode` is the comma-joined list of DISTINCT route codes run on that
 * date - a single date can legitimately have multiple different routes
 * (that's exactly why this row is consolidated rather than one-row-per-
 * record), so this is intentionally a list, not a single value.
 *
 * `totalKm` is the SUM of route.km across every record for that date -
 * including repeats if the same route was run more than once that day -
 * matching how totalAmount is already summed regardless of route
 * repetition (two trips on the same route means twice the distance
 * driven, twice the amount earned).
 */
@Getter
@Builder
@AllArgsConstructor
public class DailyRoutePaymentReceiptRow {
    private LocalDate date;
    private String routeCode;
    private BigDecimal totalKm;
    private Integer loadCount;
    private BigDecimal priceRate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
}