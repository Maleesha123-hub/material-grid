package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.PriceRateCreateRequest;
import com.pixelMind.materialGrid.dto.request.PriceRateUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PriceRateResponse;
import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.mapper.PriceRateMapper;
import com.pixelMind.materialGrid.repository.PriceRateRepository;
import com.pixelMind.materialGrid.service.impl.PriceRateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceRateServiceImplTest {

    @Mock
    private PriceRateRepository priceRateRepository;
    @Mock
    private PriceRateMapper priceRateMapper;

    @InjectMocks
    private PriceRateServiceImpl priceRateService;

    @Test
    void createActiveRate_deactivatesPreviousActiveRate() {
        PriceRate previousActive = PriceRate.builder()
                .id(1L).price(new BigDecimal("100.00")).status(PriceRateStatus.ACTIVE).build();

        when(priceRateRepository.findActiveForUpdate()).thenReturn(Optional.of(previousActive));
        when(priceRateRepository.save(any(PriceRate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceRateMapper.toResponse(any(PriceRate.class))).thenReturn(
                PriceRateResponse.builder().id(2L).status(PriceRateStatus.ACTIVE).build());

        PriceRateCreateRequest request = new PriceRateCreateRequest(new BigDecimal("150.00"), PriceRateStatus.ACTIVE);
        PriceRateResponse response = priceRateService.createPriceRate(request);

        assertThat(response.getStatus()).isEqualTo(PriceRateStatus.ACTIVE);
        assertThat(previousActive.getStatus()).isEqualTo(PriceRateStatus.INACTIVE);

        ArgumentCaptor<PriceRate> savedCaptor = ArgumentCaptor.forClass(PriceRate.class);
        verify(priceRateRepository, times(2)).save(savedCaptor.capture());
        List<PriceRate> saved = savedCaptor.getAllValues();
        assertThat(saved).extracting(PriceRate::getStatus)
                .containsExactly(PriceRateStatus.INACTIVE, PriceRateStatus.ACTIVE);
    }

    @Test
    void createInactiveRate_doesNotTouchExistingActiveRate() {
        when(priceRateRepository.save(any(PriceRate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceRateMapper.toResponse(any(PriceRate.class))).thenReturn(
                PriceRateResponse.builder().id(3L).status(PriceRateStatus.INACTIVE).build());

        PriceRateCreateRequest request = new PriceRateCreateRequest(new BigDecimal("200.00"), PriceRateStatus.INACTIVE);
        priceRateService.createPriceRate(request);

        verify(priceRateRepository, never()).findActiveForUpdate();
        verify(priceRateRepository, times(1)).save(any(PriceRate.class));
    }

    @Test
    void updateToActive_autoDeactivatesPrevious() {
        PriceRate target = PriceRate.builder()
                .id(2L).price(new BigDecimal("10.00")).status(PriceRateStatus.INACTIVE).build();
        PriceRate previousActive = PriceRate.builder()
                .id(1L).price(new BigDecimal("5.00")).status(PriceRateStatus.ACTIVE).build();

        when(priceRateRepository.findById(2L)).thenReturn(Optional.of(target));
        when(priceRateRepository.findActiveForUpdate()).thenReturn(Optional.of(previousActive));
        when(priceRateRepository.save(any(PriceRate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceRateMapper.toResponse(any(PriceRate.class))).thenReturn(
                PriceRateResponse.builder().id(2L).status(PriceRateStatus.ACTIVE).build());

        PriceRateUpdateRequest request = new PriceRateUpdateRequest(new BigDecimal("12.00"), PriceRateStatus.ACTIVE);
        priceRateService.updatePriceRate(2L, request);

        assertThat(previousActive.getStatus()).isEqualTo(PriceRateStatus.INACTIVE);
        assertThat(target.getStatus()).isEqualTo(PriceRateStatus.ACTIVE);
    }

    @Test
    void deactivatingSoleActiveRate_isRejected() {
        PriceRate active = PriceRate.builder()
                .id(1L).price(new BigDecimal("10.00")).status(PriceRateStatus.ACTIVE).build();
        when(priceRateRepository.findById(1L)).thenReturn(Optional.of(active));

        PriceRateUpdateRequest request = new PriceRateUpdateRequest(new BigDecimal("10.00"), PriceRateStatus.INACTIVE);

        assertThatThrownBy(() -> priceRateService.updatePriceRate(1L, request))
                .isInstanceOf(BusinessException.class);

        verify(priceRateRepository, never()).save(any());
    }

    @Test
    void deleteActiveRate_isRejected() {
        PriceRate active = PriceRate.builder()
                .id(1L).price(new BigDecimal("10.00")).status(PriceRateStatus.ACTIVE).build();
        when(priceRateRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> priceRateService.deletePriceRate(1L))
                .isInstanceOf(BusinessException.class);

        verify(priceRateRepository, never()).delete(any());
    }

    @Test
    void getActivePriceRate_whenNoneActive_throwsNotFound() {
        when(priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceRateService.getActivePriceRate())
                .isInstanceOf(com.pixelMind.materialGrid.exception.ResourceNotFoundException.class);
    }
}
