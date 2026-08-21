package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.request.DailyRouteCreateRequest;
import com.pixelMind.materialGrid.dto.request.DailyRouteUpdateRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.DailyRouteResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.service.DailyRouteImportService;
import com.pixelMind.materialGrid.service.DailyRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "Daily Routes", description = "Vehicle+Route+active-PriceRate billing records; amount and price rate are always server-resolved")
@RestController
@RequestMapping("/api/v1/daily-routes")
@RequiredArgsConstructor
public class DailyRouteController {

    private final DailyRouteService dailyRouteService;
    private final DailyRouteImportService dailyRouteImportService;

    @Operation(summary = "Create a daily route. priceRateId is never accepted - the active PriceRate is resolved and amount computed server-side.")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyRouteResponse>> create(@Valid @RequestBody DailyRouteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Daily route created successfully", dailyRouteService.createDailyRoute(request)));
    }

    @Operation(summary = "Bulk-upload daily routes from an Excel file (Date | Vehicle Number | Route Code | Check By). All-or-nothing.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkUploadResponse>> upload(@RequestParam("file") MultipartFile file) {
        BulkUploadResponse result = dailyRouteImportService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    @Operation(summary = "Search daily routes (paginated; filter by date, vehicleId, routeId, priceRateId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DailyRouteResponse>>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long priceRateId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        PageResponse<DailyRouteResponse> page =
                new PageResponse<>(dailyRouteService.search(date, vehicleId, routeId, priceRateId, pageable));

        return ResponseEntity.ok(ApiResponse.success("Daily routes retrieved successfully", page));
    }

    @Operation(summary = "Get a daily route by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyRouteResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Daily route retrieved successfully", dailyRouteService.getDailyRoute(id)));
    }

    @Operation(summary = "Update a daily route. priceRateId is never accepted; the active PriceRate and amount are re-resolved.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyRouteResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DailyRouteUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Daily route updated successfully", dailyRouteService.updateDailyRoute(id, request)));
    }

    @Operation(summary = "Delete (soft-delete) a daily route")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        dailyRouteService.deleteDailyRoute(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}