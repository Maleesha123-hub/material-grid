//package com.pixelMind.materialGrid.controller;
//
//import com.pixelMind.materialGrid.dto.request.VehicleLicenseCreateRequest;
//import com.pixelMind.materialGrid.dto.request.VehicleLicenseUpdateRequest;
//import com.pixelMind.materialGrid.dto.response.ApiResponse;
//import com.pixelMind.materialGrid.dto.response.PageResponse;
//import com.pixelMind.materialGrid.dto.response.VehicleLicenseResponse;
//import com.pixelMind.materialGrid.entity.enums.VehicleLicenseStatus;
//import com.pixelMind.materialGrid.service.VehicleLicenseService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@Tag(name = "Vehicle Licenses", description = "Vehicle-to-License assignment records (date + status), supports repeat/renewal assignments")
//@RestController
//@RequestMapping("/api/v1/vehicle-licenses")
//@RequiredArgsConstructor
//public class VehicleLicenseController {
//
//    private final VehicleLicenseService vehicleLicenseService;
//
//    @Operation(summary = "Assign a license to a vehicle")
//    @PostMapping
//    public ResponseEntity<ApiResponse<VehicleLicenseResponse>> create(@Valid @RequestBody VehicleLicenseCreateRequest request) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Vehicle license created successfully", vehicleLicenseService.createVehicleLicense(request)));
//    }
//
//    @Operation(summary = "List vehicle-license assignments (paginated, filterable by status)")
//    @GetMapping
//    public ResponseEntity<ApiResponse<PageResponse<VehicleLicenseResponse>>> getAll(
//            @RequestParam(required = false) VehicleLicenseStatus status,
//            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
//        PageResponse<VehicleLicenseResponse> page =
//                new PageResponse<>(vehicleLicenseService.getVehicleLicenses(status, pageable));
//        return ResponseEntity.ok(ApiResponse.success("Vehicle licenses retrieved successfully", page));
//    }
//
//    @Operation(summary = "Get a vehicle-license assignment by id")
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<VehicleLicenseResponse>> getOne(@PathVariable Long id) {
//        return ResponseEntity.ok(ApiResponse.success("Vehicle license retrieved successfully", vehicleLicenseService.getVehicleLicense(id)));
//    }
//
//    @Operation(summary = "List assignments for a specific vehicle")
//    @GetMapping("/vehicle/{vehicleId}")
//    public ResponseEntity<ApiResponse<PageResponse<VehicleLicenseResponse>>> getByVehicle(
//            @PathVariable Long vehicleId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
//        PageResponse<VehicleLicenseResponse> page = new PageResponse<>(vehicleLicenseService.getByVehicle(vehicleId, pageable));
//        return ResponseEntity.ok(ApiResponse.success("Vehicle's licenses retrieved successfully", page));
//    }
//
//    @Operation(summary = "List assignments for a specific license")
//    @GetMapping("/license/{licenseId}")
//    public ResponseEntity<ApiResponse<PageResponse<VehicleLicenseResponse>>> getByLicense(
//            @PathVariable Long licenseId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
//        PageResponse<VehicleLicenseResponse> page = new PageResponse<>(vehicleLicenseService.getByLicense(licenseId, pageable));
//        return ResponseEntity.ok(ApiResponse.success("License's vehicles retrieved successfully", page));
//    }
//
//    @Operation(summary = "Update a vehicle-license assignment's date/status (vehicle/license are immutable)")
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<VehicleLicenseResponse>> update(
//            @PathVariable Long id, @Valid @RequestBody VehicleLicenseUpdateRequest request) {
//        return ResponseEntity.ok(ApiResponse.success("Vehicle license updated successfully", vehicleLicenseService.updateVehicleLicense(id, request)));
//    }
//
//    @Operation(summary = "Delete a vehicle-license assignment")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
//        vehicleLicenseService.deleteVehicleLicense(id);
//        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
//    }
//}
