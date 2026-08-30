package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.DailyRouteCreateRequest;
import com.pixelMind.materialGrid.dto.request.DailyRouteUpdateRequest;
import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface DailyRouteService {

    DailyRouteResponse createDailyRoute(DailyRouteCreateRequest request);

    DailyRouteResponse getDailyRoute(Long id);

    Page<DailyRouteResponse> search(
            LocalDate date,
            LocalDate createdDate,
            String billNumber,
            Long vehicleId,
            Long routeId,
            Long fileHistoryId,
            Pageable pageable);

    DailyRouteResponse updateDailyRoute(Long id, DailyRouteUpdateRequest request);

    void deleteDailyRoute(Long id);
}
