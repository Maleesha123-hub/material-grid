package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.VehicleLicenseResponse;
import com.pixelMind.materialGrid.entity.VehicleLicense;
import org.springframework.stereotype.Component;

@Component
public class VehicleLicenseMapper {

    public VehicleLicenseResponse toResponse(VehicleLicense vehicleLicense) {
        if (vehicleLicense == null) {
            return null;
        }
        return VehicleLicenseResponse.builder()
                .id(vehicleLicense.getId())
                .vehicleId(vehicleLicense.getVehicle().getId())
                .vehicleNumber(vehicleLicense.getVehicle().getVehicleNumber())
                .licenseId(vehicleLicense.getLicense().getId())
                .licenseCode(vehicleLicense.getLicense().getLicenseCode())
                .date(vehicleLicense.getDate())
                .status(vehicleLicense.getStatus())
                .createdBy(vehicleLicense.getCreatedBy())
                .createdDate(vehicleLicense.getCreatedDate())
                .modifiedBy(vehicleLicense.getModifiedBy())
                .modifiedDate(vehicleLicense.getModifiedDate())
                .build();
    }
}
