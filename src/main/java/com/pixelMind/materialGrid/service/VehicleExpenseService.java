package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.VehicleExpenseCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleExpenseUpdateRequest;
import com.pixelMind.materialGrid.dto.response.VehicleExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface VehicleExpenseService {

    VehicleExpenseResponse createVehicleExpense(VehicleExpenseCreateRequest request);

    VehicleExpenseResponse getVehicleExpense(Long id);

    Page<VehicleExpenseResponse> getVehicleExpenses(LocalDate from, LocalDate to, Pageable pageable);

    Page<VehicleExpenseResponse> getByVehicle(Long vehicleId, Pageable pageable);

    VehicleExpenseResponse updateVehicleExpense(Long id, VehicleExpenseUpdateRequest request);

    void deleteVehicleExpense(Long id);
}
