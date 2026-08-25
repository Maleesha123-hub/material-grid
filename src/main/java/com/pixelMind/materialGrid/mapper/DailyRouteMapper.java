package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.dto.response.PriceRateSummaryResponse;
import com.pixelMind.materialGrid.dto.response.RouteSummaryResponse;
import com.pixelMind.materialGrid.dto.response.VehicleSummaryResponse;
import com.pixelMind.materialGrid.entity.DailyRoute;
import org.springframework.stereotype.Component;

@Component
public class DailyRouteMapper {

    public DailyRouteResponse toResponse(DailyRoute dailyRoute) {
        if (dailyRoute == null) {
            return null;
        }
        return DailyRouteResponse.builder()
                .id(dailyRoute.getId())
                .date(dailyRoute.getDate())
                .vehicle(VehicleSummaryResponse.builder()
                        .id(dailyRoute.getVehicle().getId())
                        .vehicleNumber(dailyRoute.getVehicle().getVehicleNumber())
                        .build())
                .route(RouteSummaryResponse.builder()
                        .id(dailyRoute.getRoute().getId())
                        .routeCode(dailyRoute.getRoute().getRouteCode())
                        .build())
                .amount(dailyRoute.getAmount())
                .checkBy(dailyRoute.getCheckBy())
                .bilNumber(dailyRoute.getBillNumber())
                .createdBy(dailyRoute.getCreatedBy())
                .createdDate(dailyRoute.getCreatedDate())
                .modifiedBy(dailyRoute.getModifiedBy())
                .modifiedDate(dailyRoute.getModifiedDate())
                .build();
    }
}