package com.pixelMind.materialGrid.dto.response;

import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class VehicleLicenseResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleNumber;
    private Long licenseId;
    private String licenseCode;
    private LocalDate date;
    private VehicleLicenseStatus status;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}
