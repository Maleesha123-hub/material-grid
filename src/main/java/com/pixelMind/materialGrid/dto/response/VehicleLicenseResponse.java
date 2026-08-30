package com.pixelMind.materialGrid.dto.response;

import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
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
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDate date;
    private LocalDate assignDate;
    private VehicleLicenseStatus status;
    private Long fileHistoryId;
    private FileHistoryResponse fileHistory;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}
