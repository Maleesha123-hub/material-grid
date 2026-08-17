//package com.pixelMind.materialGrid.controller;
//
//import com.pixelMind.materialGrid.dto.request.VehicleExpenseCreateRequest;
//import com.pixelMind.materialGrid.dto.request.VehicleExpenseUpdateRequest;
//import com.pixelMind.materialGrid.dto.response.ApiResponse;
//import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
//import com.pixelMind.materialGrid.dto.response.PageResponse;
//import com.pixelMind.materialGrid.dto.response.VehicleExpenseResponse;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDate;
//
//@Tag(name = "Vehicle Expenses", description = "Per-vehicle expense records (soft-deleted to preserve financial history)")
//@RestController
//@RequestMapping("/api/v1/vehicle-expenses")
//@RequiredArgsConstructor
//public class VehicleExpenseController {
//
//    private final VehicleExpenseService vehicleExpenseService;
//    private final VehicleExpenseImportService vehicleExpenseImportService;
//
//    @Operation(summary = "Create a vehicle expense record")
//    @PostMapping
//    public ResponseEntity<ApiResponse<VehicleExpenseResponse>> create(@Valid @RequestBody VehicleExpenseCreateRequest request) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Vehicle expense created successfully", vehicleExpenseService.createVehicleExpense(request)));
//    }
//
//    @Operation(summary = "Bulk-upload vehicle expenses from an Excel file (Date | Vehicle Number | Expense). All-or-nothing.")
//    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<ApiResponse<BulkUploadResponse>> upload(@RequestParam("file") MultipartFile file) {
//        BulkUploadResponse result = vehicleExpenseImportService.importFromExcel(file);
//        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
//    }
//
//    @Operation(summary = "List vehicle expenses (paginated, filterable by date range)")
//    @GetMapping
//    public ResponseEntity<ApiResponse<PageResponse<VehicleExpenseResponse>>> getAll(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
//            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
//        PageResponse<VehicleExpenseResponse> page = new PageResponse<>(vehicleExpenseService.getVehicleExpenses(from, to, pageable));
//        return ResponseEntity.ok(ApiResponse.success("Vehicle expenses retrieved successfully", page));
//    }
//
//    @Operation(summary = "Get a vehicle expense by id")
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<VehicleExpenseResponse>> getOne(@PathVariable Long id) {
//        return ResponseEntity.ok(ApiResponse.success("Vehicle expense retrieved successfully", vehicleExpenseService.getVehicleExpense(id)));
//    }
//
//    @Operation(summary = "List expenses for a specific vehicle")
//    @GetMapping("/vehicle/{vehicleId}")
//    public ResponseEntity<ApiResponse<PageResponse<VehicleExpenseResponse>>> getByVehicle(
//            @PathVariable Long vehicleId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
//        PageResponse<VehicleExpenseResponse> page = new PageResponse<>(vehicleExpenseService.getByVehicle(vehicleId, pageable));
//        return ResponseEntity.ok(ApiResponse.success("Vehicle's expenses retrieved successfully", page));
//    }
//
//    @Operation(summary = "Update a vehicle expense (vehicle association is immutable)")
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<VehicleExpenseResponse>> update(
//            @PathVariable Long id, @Valid @RequestBody VehicleExpenseUpdateRequest request) {
//        return ResponseEntity.ok(ApiResponse.success("Vehicle expense updated successfully", vehicleExpenseService.updateVehicleExpense(id, request)));
//    }
//
//    @Operation(summary = "Delete (soft-delete) a vehicle expense")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
//        vehicleExpenseService.deleteVehicleExpense(id);
//        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
//    }
//}