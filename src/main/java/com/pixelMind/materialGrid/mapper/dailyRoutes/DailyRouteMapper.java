package com.pixelMind.materialGrid.mapper.dailyRoutes;

import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.dto.response.PriceRateSummaryResponse;
import com.pixelMind.materialGrid.dto.response.RouteSummaryResponse;
import com.pixelMind.materialGrid.dto.response.VehicleSummaryResponse;
import com.pixelMind.materialGrid.entity.dailyRoutes.DailyRoute;
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
                .vehicle(dailyRoute.getVehicle() != null ? VehicleSummaryResponse.builder()
                        .id(dailyRoute.getVehicle().getId())
                        .vehicleNumber(dailyRoute.getVehicle().getVehicleNumber())
                        .build() : null)
                .route(dailyRoute.getRoute() != null ? RouteSummaryResponse.builder()
                        .id(dailyRoute.getRoute().getId())
                        .routeCode(dailyRoute.getRoute().getRouteCode())
                        .build() : null)
                .createdBy(dailyRoute.getCreatedBy())
                .createdDate(dailyRoute.getCreatedDate())
                .modifiedBy(dailyRoute.getModifiedBy())
                .modifiedDate(dailyRoute.getModifiedDate())
                .build();
    }
}