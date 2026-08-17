package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.request.VehicleCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleUpdateRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import com.pixelMind.materialGrid.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vehicles", description = "Vehicle management with search and CRUD operations")
@RestController
@RequestMapping(value = "/api/material-grid/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "Search vehicles (auto-complete / dropdown query)")
    @GetMapping(value = "/search")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> search(@RequestParam(required = false) String query) {
        List<VehicleResponse> vehicles = vehicleService.searchVehicles(query);
        return ResponseEntity.ok(ApiResponse.success("Vehicles retrieved successfully", vehicles));
    }

    @Operation(summary = "List vehicles (paginated, searchable by vehicle number)")
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

    @Operation(summary = "Create a vehicle")
    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(@Valid @RequestBody VehicleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle created successfully", vehicleService.createVehicle(request)));
    }

    @Operation(summary = "Update a vehicle")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated successfully", vehicleService.updateVehicle(id, request)));
    }

    @Operation(summary = "Delete a vehicle")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
