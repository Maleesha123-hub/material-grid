package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.RouteCreateRequest;
import com.pixelMind.materialGrid.dto.response.RouteResponse;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.mapper.RouteMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.service.impl.RouteServiceImpl;
import com.pixelMind.materialGrid.util.CodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private DailyRouteRepository dailyRouteRepository;
    @Mock
    private RouteMapper routeMapper;
    @Mock
    private CodeGeneratorService codeGeneratorService;

    @InjectMocks
    private RouteServiceImpl routeService;

    @Test
    void createRoute_usesGeneratedCode_neverClientSupplied() {
        when(codeGeneratorService.nextCode(anyString(), anyString(), anyInt())).thenReturn("RT000042");
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> {
            Route r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(routeMapper.toResponse(any(Route.class))).thenAnswer(inv -> {
            Route r = inv.getArgument(0);
            return RouteResponse.builder().id(r.getId()).routeCode(r.getRouteCode()).build();
        });

        RouteCreateRequest request = new RouteCreateRequest("A", "B", new BigDecimal("12.50"), true);
        RouteResponse response = routeService.createRoute(request);

        assertThat(response.getRouteCode()).isEqualTo("RT000042");
        verify(codeGeneratorService).nextCode("ROUTE_CODE", "RT", 6);
    }

    @Test
    void deleteRoute_withExistingDailyRoutes_isRejected() {
        Route route = Route.builder().id(1L).routeCode("RT000001").build();
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(dailyRouteRepository.existsByRouteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> routeService.deleteRoute(1L))
                .isInstanceOf(BusinessException.class);

        verify(routeRepository, never()).delete(any());
    }

    @Test
    void deleteRoute_withNoDailyRoutes_succeeds() {
        Route route = Route.builder().id(2L).routeCode("RT000002").build();
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route));
        when(dailyRouteRepository.existsByRouteId(2L)).thenReturn(false);

        routeService.deleteRoute(2L);

        verify(routeRepository).delete(route);
    }
}
