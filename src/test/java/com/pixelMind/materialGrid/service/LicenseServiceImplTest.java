package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.LicenseCreateRequest;
import com.pixelMind.materialGrid.dto.response.LicenseResponse;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.mapper.LicenseMapper;
import com.pixelMind.materialGrid.repository.LicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.service.impl.LicenseServiceImpl;
import com.pixelMind.materialGrid.util.CodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LicenseServiceImplTest {

    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private VehicleLicenseRepository vehicleLicenseRepository;
    @Mock
    private LicenseMapper licenseMapper;
    @Mock
    private CodeGeneratorService codeGeneratorService;

    @InjectMocks
    private LicenseServiceImpl licenseService;

    @Test
    void createLicense_endDateBeforeStartDate_isRejected() {
        LicenseCreateRequest request = new LicenseCreateRequest(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), new BigDecimal("100.00"));

        assertThatThrownBy(() -> licenseService.createLicense(request))
                .isInstanceOf(BusinessException.class);

        verify(licenseRepository, never()).save(any());
    }

    @Test
    void createLicense_success_usesGeneratedCode() {
        when(codeGeneratorService.nextCode(anyString(), anyString(), anyInt())).thenReturn("LIC000001");
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));
        when(licenseMapper.toResponse(any(License.class))).thenAnswer(inv -> {
            License l = inv.getArgument(0);
            return LicenseResponse.builder().licenseCode(l.getLicenseCode()).build();
        });

        LicenseCreateRequest request = new LicenseCreateRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), new BigDecimal("500.00"));
        LicenseResponse response = licenseService.createLicense(request);

        assertThat(response.getLicenseCode()).isEqualTo("LIC000001");
    }
}
