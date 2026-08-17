package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.RouteResponse;
import com.pixelMind.materialGrid.entity.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteResponse toResponse(Route route) {
        if (route == null) {
            return null;
        }
        return RouteResponse.builder()
                .id(route.getId())
                .routeCode(route.getRouteCode())
                .startLocation(route.getStartLocation())
                .endLocation(route.getEndLocation())
                .km(route.getKm())
                .createdBy(route.getCreatedBy())
                .createdDate(route.getCreatedDate())
                .modifiedBy(route.getModifiedBy())
                .modifiedDate(route.getModifiedDate())
                .build();
    }
}
