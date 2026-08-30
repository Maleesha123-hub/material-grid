package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.VehicleLicenseResponse;
import com.pixelMind.materialGrid.entity.VehicleLicense;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class VehicleLicenseMapper {

    private final FileHistoryMapper fileHistoryMapper;

    public VehicleLicenseResponse toResponse(VehicleLicense vehicleLicense) {
        if (vehicleLicense == null) {
            return null;
        }
        BigDecimal price = vehicleLicense.getLicense() != null ? vehicleLicense.getLicense().getPrice() : null;
        return VehicleLicenseResponse.builder()
                .id(vehicleLicense.getId())
                .vehicleId(vehicleLicense.getVehicle().getId())
                .vehicleNumber(vehicleLicense.getVehicle().getVehicleNumber())
                .licenseId(vehicleLicense.getLicense().getId())
                .licenseCode(vehicleLicense.getLicense().getLicenseCode())
                .price(price)
                .amount(price)
                .date(vehicleLicense.getDate())
                .assignDate(vehicleLicense.getDate())
                .status(vehicleLicense.getStatus())
                .fileHistoryId(vehicleLicense.getFileHistory() != null ? vehicleLicense.getFileHistory().getId() : null)
                .fileHistory(fileHistoryMapper.toResponse(vehicleLicense.getFileHistory()))
                .createdBy(vehicleLicense.getCreatedBy())
                .createdDate(vehicleLicense.getCreatedDate())
                .modifiedBy(vehicleLicense.getModifiedBy())
                .modifiedDate(vehicleLicense.getModifiedDate())
                .build();
    }
}
