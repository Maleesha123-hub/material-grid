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
                .build();
    }
}
