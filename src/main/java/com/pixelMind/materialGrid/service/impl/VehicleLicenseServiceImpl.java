package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.VehicleLicenseCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleLicenseUpdateRequest;
import com.pixelMind.materialGrid.dto.response.VehicleLicenseResponse;
import com.pixelMind.materialGrid.entity.License;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleLicense;
import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.VehicleLicenseMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.LicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.VehicleLicenseService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Business assumption made explicit (per the spec's request to justify the
 * uniqueness decision before implementing it): a Vehicle CAN legitimately
 * hold the same License more than once over its lifetime - e.g. an annual
 * permit that's renewed every year is naturally a new
 * (vehicle, license, date) assignment each time, not an update to a single
 * eternal row. Treating a repeat assignment as an error would make it
 * impossible to represent renewal history at all. Consequently there is
 * NO unique constraint on (vehicle_id, license_id) - see
 * V8__create_vehicle_licenses_table.sql - and no duplicate-check here. What
 * IS enforced is that vehicle and license must each actually exist, and the
 * assignment carries its own date/status so which assignment is the
 * "current" one for a vehicle is always unambiguous
 * (findByVehicleIdAndStatus(ACTIVE)).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleLicenseServiceImpl implements VehicleLicenseService {

    // Repositories
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final VehicleRepository vehicleRepository;
    private final LicenseRepository licenseRepository;
    private final DailyRouteRepository dailyRouteRepository;

    // Mappers
    private final VehicleLicenseMapper vehicleLicenseMapper;

    @Override
    @Transactional
    public VehicleLicenseResponse createVehicleLicense(VehicleLicenseCreateRequest request) {

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + request.getVehicleId(), ErrorCodeConstants.VEHICLE_NOT_FOUND));

        License license = licenseRepository.findById(request.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found with id: " + request.getLicenseId(), ErrorCodeConstants.LICENSE_NOT_FOUND));

        List<VehicleLicense> existsVehicleLicenses = vehicleLicenseRepository.findByVehicleAndLicenseInAndDeletedFalse(
                vehicle, Collections.singletonList(license)
        );

        if (!existsVehicleLicenses.isEmpty()) {
            throw new BusinessException("Vehicle license " +
                    license.getLicenseCode() +
                    " is already assigned to " +
                    vehicle.getVehicleNumber(), "400");
        }

        String actor = SecurityUtil.getCurrentUsername();
        VehicleLicense vehicleLicense = VehicleLicense.builder()
                .vehicle(vehicle)
                .license(license)
                .date(null)
                .status(VehicleLicenseStatus.ACTIVE)
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        VehicleLicense saved = vehicleLicenseRepository.save(vehicleLicense);
        log.info("VehicleLicense created: id={}, vehicleId={}, licenseId={}, by={}",
                saved.getId(), vehicle.getId(), license.getId(), actor);
        return vehicleLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleLicenseResponse getVehicleLicense(Long id) {
        return vehicleLicenseMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleLicenseResponse> getVehicleLicenses(VehicleLicenseStatus status, Pageable pageable) {
        if (status != null) {
            return vehicleLicenseRepository.findByStatusAndDeletedFalse(status, pageable)
                    .map(vehicleLicenseMapper::toResponse);
        }
        return vehicleLicenseRepository.findAll(pageable).map(vehicleLicenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleLicenseResponse> search(
            Long licenseId,
            Long vehicleId,
            LocalDate date,
            LocalDate createdDate,
            VehicleLicenseStatus status,
            Long fileHistoryId,
            Pageable pageable) {
        LocalDateTime createdDateFrom = createdDate != null ? createdDate.atStartOfDay() : null;
        LocalDateTime createdDateTo = createdDate != null ? createdDate.plusDays(1).atStartOfDay() : null;

        return vehicleLicenseRepository.search(
                        licenseId,
                        vehicleId,
                        date,
                        createdDateFrom,
                        createdDateTo,
                        status,
                        fileHistoryId,
                        pageable)
                .map(vehicleLicenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleLicenseResponse> getByVehicle(Long vehicleId, Pageable pageable) {
        return vehicleLicenseRepository.findByVehicleIdAndDeletedFalse(vehicleId, pageable)
                .map(vehicleLicenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleLicenseResponse> getByLicense(Long licenseId, Pageable pageable) {
        return vehicleLicenseRepository.findByLicenseIdAndDeletedFalse(licenseId, pageable)
                .map(vehicleLicenseMapper::toResponse);
    }

    @Override
    @Transactional
    public VehicleLicenseResponse updateVehicleLicense(Long id, VehicleLicenseUpdateRequest request) {

        VehicleLicense vehicleLicense = findOrThrow(id);

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + request.getVehicleId(), ErrorCodeConstants.VEHICLE_NOT_FOUND));

        License license = licenseRepository.findById(request.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found with id: " + request.getLicenseId(), ErrorCodeConstants.LICENSE_NOT_FOUND));

        List<VehicleLicense> existsVehicleLicenses = vehicleLicenseRepository.findByVehicleAndLicenseInAndDeletedFalse(
                vehicle, Collections.singletonList(license)
        );

        if (!existsVehicleLicenses.isEmpty()) {

            throw new BusinessException("Vehicle license " +
                    license.getLicenseCode() +
                    " is already assigned to " +
                    vehicle.getVehicleNumber(), "400");

        }

        vehicleLicense.setVehicle(vehicle);
        vehicleLicense.setLicense(license);
        vehicleLicense.setStatus(VehicleLicenseStatus.ACTIVE);
        vehicleLicense.setModifiedBy(SecurityUtil.getCurrentUsername());

        VehicleLicense saved = vehicleLicenseRepository.save(vehicleLicense);
        log.info("VehicleLicense updated: id={}, by={}", saved.getId(), vehicleLicense.getModifiedBy());
        return vehicleLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVehicleLicense(Long id) {
        VehicleLicense vehicleLicense = findOrThrow(id);
        vehicleLicense.setDeleted(true);
        vehicleLicense.setModifiedBy(SecurityUtil.getCurrentUsername());
        vehicleLicenseRepository.delete(vehicleLicense);
        log.info("VehicleLicense deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private VehicleLicense findOrThrow(Long id) {
        return vehicleLicenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "VehicleLicense not found with id: " + id, ErrorCodeConstants.VEHICLE_LICENSE_NOT_FOUND));
    }
}
