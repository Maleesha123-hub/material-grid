package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.LicenseCreateRequest;
import com.pixelMind.materialGrid.dto.request.LicenseUpdateRequest;
import com.pixelMind.materialGrid.dto.response.LicenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LicenseService {

    LicenseResponse createLicense(LicenseCreateRequest request);

    LicenseResponse getLicense(Long id);

    Page<LicenseResponse> getLicenses(LocalDate startDate, LocalDate endDate, Pageable pageable);

    LicenseResponse updateLicense(Long id, LicenseUpdateRequest request);

    void deleteLicense(Long id);
}
