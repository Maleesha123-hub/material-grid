//package com.pixelMind.materialGrid.service;
//
//import com.pixelMind.materialGrid.dto.request.PriceRateCreateRequest;
//import com.pixelMind.materialGrid.dto.request.PriceRateUpdateRequest;
//import com.pixelMind.materialGrid.dto.response.PriceRateResponse;
//import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
//public interface PriceRateService {
//
//    PriceRateResponse createPriceRate(PriceRateCreateRequest request);
//
//    PriceRateResponse getPriceRate(Long id);
//
//    Page<PriceRateResponse> getPriceRates(PriceRateStatus statusFilter, Pageable pageable);
//
//    PriceRateResponse updatePriceRate(Long id, PriceRateUpdateRequest request);
//
//    void deletePriceRate(Long id);
//
//    PriceRateResponse getActivePriceRate();
//}
