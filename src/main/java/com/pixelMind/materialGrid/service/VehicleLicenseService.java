//package com.pixelMind.materialGrid.service;
//
//import com.pixelMind.materialGrid.dto.request.VehicleLicenseCreateRequest;
//import com.pixelMind.materialGrid.dto.request.VehicleLicenseUpdateRequest;
//import com.pixelMind.materialGrid.dto.response.VehicleLicenseResponse;
//import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
//public interface VehicleLicenseService {
//
//    VehicleLicenseResponse createVehicleLicense(VehicleLicenseCreateRequest request);
//
//    VehicleLicenseResponse getVehicleLicense(Long id);
//
//    Page<VehicleLicenseResponse> getVehicleLicenses(VehicleLicenseStatus status, Pageable pageable);
//
//    Page<VehicleLicenseResponse> getByVehicle(Long vehicleId, Pageable pageable);
//
//    Page<VehicleLicenseResponse> getByLicense(Long licenseId, Pageable pageable);
//
//    VehicleLicenseResponse updateVehicleLicense(Long id, VehicleLicenseUpdateRequest request);
//
//    void deleteVehicleLicense(Long id);
//}
