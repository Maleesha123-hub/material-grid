/*
package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.VehicleCreateRequest;
import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.mapper.VehicleMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleExpenseRepository vehicleExpenseRepository;
    @Mock
    private VehicleLicenseRepository vehicleLicenseRepository;
    @Mock
    private DailyRouteRepository dailyRouteRepository;
    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    void createVehicle_duplicateNumber_throws() {
        when(vehicleRepository.existsByVehicleNumber("ABC-1234")).thenReturn(true);

        VehicleCreateRequest request = new VehicleCreateRequest("ABC-1234", new BigDecimal("2.5"));

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createVehicle_success() {
        when(vehicleRepository.existsByVehicleNumber("WP-9999")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleMapper.toResponse(any(Vehicle.class))).thenReturn(
                VehicleResponse.builder().vehicleNumber("WP-9999").build());

        VehicleCreateRequest request = new VehicleCreateRequest("wp-9999", new BigDecimal("3.0"));
        VehicleResponse response = vehicleService.createVehicle(request);

        verify(vehicleRepository).save(argThat(v -> v.getVehicleNumber().equals("WP-9999")));
    }

    @Test
    void deleteVehicle_withDependentRecords_isRejected() {
        Vehicle vehicle = Vehicle.builder().id(1L).vehicleNumber("WP-1234").build();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleExpenseRepository.existsByVehicleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.deleteVehicle(1L))
                .isInstanceOf(BusinessException.class);

        verify(vehicleRepository, never()).delete(any());
    }
}
*/
