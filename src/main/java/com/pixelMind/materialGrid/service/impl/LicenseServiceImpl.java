package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.CodeSequenceConstants;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.LicenseCreateRequest;
import com.pixelMind.materialGrid.dto.request.LicenseUpdateRequest;
import com.pixelMind.materialGrid.dto.response.LicenseResponse;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.LicenseMapper;
import com.pixelMind.materialGrid.repository.LicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.service.LicenseService;
import com.pixelMind.materialGrid.util.CodeGeneratorService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRepository licenseRepository;
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final LicenseMapper licenseMapper;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional
    public LicenseResponse createLicense(LicenseCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateNoDateOverlap(null, request.getStartDate(), request.getEndDate());
        String actor = SecurityUtil.getCurrentUsername();

        String licenseCode = codeGeneratorService.nextCode(
                CodeSequenceConstants.LICENSE_CODE_SEQUENCE,
                CodeSequenceConstants.LICENSE_CODE_PREFIX,
                CodeSequenceConstants.LICENSE_CODE_PAD_LENGTH);

        License license = License.builder()
                .licenseCode(licenseCode)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .price(request.getPrice())
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        License saved = licenseRepository.save(license);
        log.info("License created: id={}, licenseCode={}, by={}", saved.getId(), saved.getLicenseCode(), actor);
        return licenseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LicenseResponse getLicense(Long id) {
        return licenseMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LicenseResponse> getLicenses(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return licenseRepository.findAllByDateRange(startDate, endDate, pageable).map(licenseMapper::toResponse);
    }

    @Override
    @Transactional
    public LicenseResponse updateLicense(Long id, LicenseUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateNoDateOverlap(id, request.getStartDate(), request.getEndDate());
        License license = findOrThrow(id);

        license.setStartDate(request.getStartDate());
        license.setEndDate(request.getEndDate());
        license.setPrice(request.getPrice());
        license.setModifiedBy(SecurityUtil.getCurrentUsername());
        // licenseCode is immutable by design - never updated here.

        License saved = licenseRepository.save(license);
        log.info("License updated: id={}, by={}", saved.getId(), license.getModifiedBy());
        return licenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteLicense(Long id) {
        License license = findOrThrow(id);
        if (vehicleLicenseRepository.existsByLicenseId(id)) {
            throw new BusinessException(
                    "Cannot delete license with existing vehicle assignment records. "
                            + "Vehicle-license assignments are historical records and this license must be preserved.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }
        licenseRepository.delete(license);
        log.info("License deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(
                    "End date must not be before start date", ErrorCodeConstants.VALIDATION_FAILED);
        }
    }

    private void validateNoDateOverlap(Long licenseId, LocalDate startDate, LocalDate endDate) {
        if (licenseRepository.existsOverlapping(licenseId, startDate, endDate)) {
            throw new BusinessException(
                    "A license already exists with an overlapping date range.",
                    ErrorCodeConstants.VALIDATION_FAILED);
        }
    }

    private License findOrThrow(Long id) {
        return licenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found with id: " + id, ErrorCodeConstants.LICENSE_NOT_FOUND));
    }
}
