/*
package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.DailyRouteCreateRequest;
import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.mapper.DailyRouteMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.PriceRateRepository;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.impl.DailyRouteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyRouteServiceImplTest {

    @Mock
    private DailyRouteRepository dailyRouteRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private PriceRateRepository priceRateRepository;
    @Mock
    private DailyRouteMapper dailyRouteMapper;

    @InjectMocks
    private DailyRouteServiceImpl dailyRouteService;

    @Test
    void createDailyRoute_computesAmountAsKmTimesPrice() {
        Vehicle vehicle = Vehicle.builder().id(1L).vehicleNumber("WP-1234").build();
        Route route = Route.builder().id(2L).routeCode("RT000001").km(new BigDecimal("10.00")).build();
        PriceRate priceRate = PriceRate.builder().id(3L).price(new BigDecimal("25.0000")).status(PriceRateStatus.ACTIVE).build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route));
        when(priceRateRepository.findById(3L)).thenReturn(Optional.of(priceRate));
        when(dailyRouteRepository.save(any(DailyRoute.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dailyRouteMapper.toResponse(any(DailyRoute.class))).thenAnswer(inv -> {
            DailyRoute d = inv.getArgument(0);
            return DailyRouteResponse.builder().amount(d.getAmount()).build();
        });

        DailyRouteCreateRequest request = new DailyRouteCreateRequest(LocalDate.of(2026, 8, 16), 1L, 3L, "");
        DailyRouteResponse response = dailyRouteService.createDailyRoute(request);

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("250.0000"));

        ArgumentCaptor<DailyRoute> captor = ArgumentCaptor.forClass(DailyRoute.class);
        verify(dailyRouteRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("250.0000");
    }

    @Test
    void createDailyRoute_inactivePriceRate_isRejected() {
        Vehicle vehicle = Vehicle.builder().id(1L).build();
        Route route = Route.builder().id(2L).km(new BigDecimal("10.00")).build();
        PriceRate inactiveRate = PriceRate.builder().id(3L).price(new BigDecimal("25.00")).status(PriceRateStatus.INACTIVE).build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route));
        when(priceRateRepository.findById(3L)).thenReturn(Optional.of(inactiveRate));

        DailyRouteCreateRequest request = new DailyRouteCreateRequest(LocalDate.of(2026, 8, 16), 1L, 3L, "");

        assertThatThrownBy(() -> dailyRouteService.createDailyRoute(request))
                .isInstanceOf(BusinessException.class);

        verify(dailyRouteRepository, never()).save(any());
    }

    @Test
    void deleteDailyRoute_softDeletesRatherThanRemoving() {
        DailyRoute dailyRoute = DailyRoute.builder().id(9L).deleted(false).build();
        when(dailyRouteRepository.findById(9L)).thenReturn(Optional.of(dailyRoute));
        when(dailyRouteRepository.save(any(DailyRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        dailyRouteService.deleteDailyRoute(9L);

        assertThat(dailyRoute.isDeleted()).isTrue();
        verify(dailyRouteRepository, never()).delete(any());
        verify(dailyRouteRepository).save(dailyRoute);
    }
}
*/
