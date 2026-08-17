package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import com.pixelMind.materialGrid.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponse toResponse(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehicleNumber(vehicle.getVehicleNumber())
                .capacity(vehicle.getCapacity())
                .createdBy(vehicle.getCreatedBy())
                .createdDate(vehicle.getCreatedDate())
                .modifiedBy(vehicle.getModifiedBy())
                .modifiedDate(vehicle.getModifiedDate())
                .build();
    }
}
