package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.RouteCreateRequest;
import com.pixelMind.materialGrid.dto.request.RouteUpdateRequest;
import com.pixelMind.materialGrid.dto.response.RouteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RouteService {

    RouteResponse createRoute(RouteCreateRequest request);

    RouteResponse getRoute(Long id);

    Page<RouteResponse> getRoutes(String search, Pageable pageable);

    RouteResponse updateRoute(Long id, RouteUpdateRequest request);

    void deleteRoute(Long id);
}
