package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.VehicleCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleUpdateRequest;
import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.VehicleMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.VehicleService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final DailyRouteRepository dailyRouteRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        String vehicleNumber = request.getVehicleNumber().toUpperCase();

        if (vehicleRepository.existsByVehicleNumber(vehicleNumber)) {
            throw new DuplicateResourceException(
                    "Vehicle number already exists: " + vehicleNumber,
                    ErrorCodeConstants.DUPLICATE_VEHICLE_NUMBER);
        }

        String actor = SecurityUtil.getCurrentUsername();
        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(vehicleNumber)
                .capacity(request.getCapacity())
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        // Application-level check above is the fast, friendly-error path;
        // the DB unique constraint on vehicle_number (see
        // V6__create_vehicles_table.sql) is the real backstop against two
        // concurrent requests both passing the check before either commits.
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}, vehicleNumber={}, by={}", saved.getId(), saved.getVehicleNumber(), actor);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long id) {
        return vehicleMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getVehicles(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return vehicleRepository.findByVehicleNumberContainingIgnoreCase(search, pageable)
                    .map(vehicleMapper::toResponse);
        }
        return vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = findOrThrow(id);
        vehicle.setCapacity(request.getCapacity());
        vehicle.setModifiedBy(SecurityUtil.getCurrentUsername());

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: id={}, by={}", saved.getId(), vehicle.getModifiedBy());
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = findOrThrow(id);

        if (vehicleExpenseRepository.existsByVehicleId(id)
                || vehicleLicenseRepository.existsByVehicleId(id)
                || dailyRouteRepository.existsByVehicleId(id)) {
            throw new BusinessException(
                    "Cannot delete vehicle with existing expense, license, or daily route records. "
                            + "These are historical records and this vehicle must be preserved for referential integrity.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }

        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + id, ErrorCodeConstants.VEHICLE_NOT_FOUND));
    }
}
