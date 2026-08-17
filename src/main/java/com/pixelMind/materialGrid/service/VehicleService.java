package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.VehicleCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleUpdateRequest;
import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(VehicleCreateRequest request);

    VehicleResponse getVehicle(Long id);

    Page<VehicleResponse> getVehicles(String search, Pageable pageable);

    List<VehicleResponse> searchVehicles(String query);

    VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request);

    void deleteVehicle(Long id);
}
