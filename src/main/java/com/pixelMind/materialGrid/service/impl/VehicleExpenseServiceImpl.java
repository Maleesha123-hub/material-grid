package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.VehicleExpenseCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleExpenseUpdateRequest;
import com.pixelMind.materialGrid.dto.response.VehicleExpenseResponse;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.VehicleExpense;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.VehicleExpenseMapper;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.VehicleExpenseService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * VehicleExpense rows are historical financial records. "Delete" is
 * implemented as a soft delete (flip {@code deleted = true}) rather than a
 * SQL DELETE, so accounting history is never destroyed by an accidental or
 * malicious API call - see VehicleExpense entity Javadoc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpenseServiceImpl implements VehicleExpenseService {

    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleExpenseMapper vehicleExpenseMapper;

    @Override
    @Transactional
    public VehicleExpenseResponse createVehicleExpense(VehicleExpenseCreateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + request.getVehicleId(), ErrorCodeConstants.VEHICLE_NOT_FOUND));

        String actor = SecurityUtil.getCurrentUsername();
        VehicleExpense expense = VehicleExpense.builder()
                .vehicle(vehicle)
                .date(request.getDate())
                .expenses(request.getExpenses())
                .deleted(false)
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        VehicleExpense saved = vehicleExpenseRepository.save(expense);
        log.info("VehicleExpense created: id={}, vehicleId={}, by={}", saved.getId(), vehicle.getId(), actor);
        return vehicleExpenseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleExpenseResponse getVehicleExpense(Long id) {
        return vehicleExpenseMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleExpenseResponse> getVehicleExpenses(LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null) {
            return vehicleExpenseRepository.findByDateBetweenAndDeletedFalse(from, to, pageable)
                    .map(vehicleExpenseMapper::toResponse);
        }
        return vehicleExpenseRepository.findByDeletedFalse(pageable).map(vehicleExpenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleExpenseResponse> getByVehicle(Long vehicleId, Pageable pageable) {
        return vehicleExpenseRepository.findByVehicleIdAndDeletedFalse(vehicleId, pageable)
                .map(vehicleExpenseMapper::toResponse);
    }

    @Override
    @Transactional
    public VehicleExpenseResponse updateVehicleExpense(Long id, VehicleExpenseUpdateRequest request) {
        VehicleExpense expense = findOrThrow(id);
        expense.setDate(request.getDate());
        expense.setExpenses(request.getExpenses());
        expense.setModifiedBy(SecurityUtil.getCurrentUsername());

        VehicleExpense saved = vehicleExpenseRepository.save(expense);
        log.info("VehicleExpense updated: id={}, by={}", saved.getId(), expense.getModifiedBy());
        return vehicleExpenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVehicleExpense(Long id) {
        VehicleExpense expense = findOrThrow(id);
        expense.setDeleted(true);
        expense.setModifiedBy(SecurityUtil.getCurrentUsername());
        vehicleExpenseRepository.save(expense);
        log.info("VehicleExpense soft-deleted: id={}, by={}", id, expense.getModifiedBy());
    }

    private VehicleExpense findOrThrow(Long id) {
        return vehicleExpenseRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle expense not found with id: " + id, ErrorCodeConstants.VEHICLE_EXPENSE_NOT_FOUND));
    }
}
