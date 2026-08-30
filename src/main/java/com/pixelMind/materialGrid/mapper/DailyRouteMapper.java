package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.dto.response.PriceRateSummaryResponse;
import com.pixelMind.materialGrid.dto.response.RouteSummaryResponse;
import com.pixelMind.materialGrid.dto.response.VehicleSummaryResponse;
import com.pixelMind.materialGrid.entity.DailyRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyRouteMapper {

    private final FileHistoryMapper fileHistoryMapper;

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
                .bilNumber(dailyRoute.getBillNumber())
                .fileHistoryId(dailyRoute.getFileHistory() != null ? dailyRoute.getFileHistory().getId() : null)
                .fileHistory(fileHistoryMapper.toResponse(dailyRoute.getFileHistory()))
                .createdBy(dailyRoute.getCreatedBy())
                .createdDate(dailyRoute.getCreatedDate())
                .modifiedBy(dailyRoute.getModifiedBy())
                .modifiedDate(dailyRoute.getModifiedDate())
                .build();
    }
}