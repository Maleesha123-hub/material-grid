package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Deliberately does not expose JPA entities. Every field here is exactly
 * what the PDF needs - the report service is responsible for resolving all
 * of it (vehicle number/capacity via Vehicle, licence fee via
 * VehicleLicense -> License, paidAmount via a SUM aggregate) before this DTO
 * is built.
 */
@Getter
@Builder
@AllArgsConstructor
public class DailyRouteReportResponse {
    private LocalDate date;
    private String vehicleNumber;
    private BigDecimal vehicleCapacity;
    private double totalVolume;
    private Integer loadCount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal licenceFee;
    private BigDecimal balance;
}