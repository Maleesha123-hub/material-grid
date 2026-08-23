package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.PriceRateResponse;
import com.pixelMind.materialGrid.entity.PriceRate;
import org.springframework.stereotype.Component;

@Component
public class PriceRateMapper {

    public PriceRateResponse toResponse(PriceRate priceRate) {
        if (priceRate == null) {
            return null;
        }
        return PriceRateResponse.builder()
                .id(priceRate.getId())
                .price(priceRate.getPrice())
                .status(priceRate.getStatus())
                .createdBy(priceRate.getCreatedBy())
                .createdDate(priceRate.getCreatedDate())
                .modifiedBy(priceRate.getModifiedBy())
                .modifiedDate(priceRate.getModifiedDate())
                .build();
    }
}
