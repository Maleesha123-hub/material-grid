package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.request.PriceRateCreateRequest;
import com.pixelMind.materialGrid.dto.request.PriceRateUpdateRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.dto.response.PriceRateResponse;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.service.PriceRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Price Rates", description = "Price rate management with single-active-rate business rules")
@RestController
@RequestMapping("/api/v1/price-rates")
@RequiredArgsConstructor
public class PriceRateController {

    private final PriceRateService priceRateService;

    @Operation(summary = "Create a price rate. If created ACTIVE, the previous active rate is auto-deactivated.")
    @PostMapping
    public ResponseEntity<ApiResponse<PriceRateResponse>> create(@Valid @RequestBody PriceRateCreateRequest request) {
        PriceRateResponse created = priceRateService.createPriceRate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Price rate created successfully", created));
    }

    @Operation(summary = "List price rates (paginated, sortable, filterable by status)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PriceRateResponse>>> getAll(
            @RequestParam(required = false) PriceRateStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<PriceRateResponse> page = new PageResponse<>(priceRateService.getPriceRates(status, pageable));
        return ResponseEntity.ok(ApiResponse.success("Price rates retrieved successfully", page));
    }

    @Operation(summary = "Get the single currently active price rate")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PriceRateResponse>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("Active price rate retrieved", priceRateService.getActivePriceRate()));
    }

    @Operation(summary = "Get a price rate by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PriceRateResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Price rate retrieved successfully", priceRateService.getPriceRate(id)));
    }

    @Operation(summary = "Update a price rate. Activating auto-deactivates the previous active rate.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PriceRateResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PriceRateUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Price rate updated successfully", priceRateService.updatePriceRate(id, request)));
    }

    @Operation(summary = "Delete a price rate (the active rate cannot be deleted)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        priceRateService.deletePriceRate(id);
        return ResponseEntity.ok(ApiResponse.success("Price rate deleted successfully", null));
    }
}
