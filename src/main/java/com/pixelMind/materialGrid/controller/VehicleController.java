package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.request.VehicleCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleUpdateRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import com.pixelMind.materialGrid.service.VehicleImportService;
import com.pixelMind.materialGrid.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * MODIFIED: added the Vehicle Excel bulk-upload endpoint. The manual
 * create/update/delete endpoints below are UNCHANGED and remain supported -
 * see this feature's architectural notes for why manual creation was kept
 * rather than removed.
 */
@Tag(name = "Vehicles", description = "Vehicle management - manual CRUD plus Excel bulk upload")
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleImportService vehicleImportService;

    @Operation(summary = "Create a vehicle manually")
    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(@Valid @RequestBody VehicleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle created successfully", vehicleService.createVehicle(request)));
    }

    @Operation(summary = "Bulk-upload vehicles from an Excel file (Vehicle Number | Capacity). All-or-nothing; rejects a filename+type already uploaded before.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkUploadResponse>> upload(@RequestParam("file") MultipartFile file) {
        BulkUploadResponse result = vehicleImportService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    @Operation(summary = "List vehicles (paginated, sortable, searchable by vehicle number)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponse>>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<VehicleResponse> page = new PageResponse<>(vehicleService.getVehicles(search, pageable));
        return ResponseEntity.ok(ApiResponse.success("Vehicles retrieved successfully", page));
    }

    @Operation(summary = "Get a vehicle by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle retrieved successfully", vehicleService.getVehicle(id)));
    }

    @Operation(summary = "Update a vehicle (vehicleNumber is immutable)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated successfully", vehicleService.updateVehicle(id, request)));
    }

    @Operation(summary = "Delete a vehicle (blocked if expense/license/daily-route records exist)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}