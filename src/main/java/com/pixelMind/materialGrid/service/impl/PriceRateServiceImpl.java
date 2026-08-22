package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.PriceRateCreateRequest;
import com.pixelMind.materialGrid.dto.request.PriceRateUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PriceRateResponse;
import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.PriceRateMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.PriceRateRepository;
import com.pixelMind.materialGrid.service.PriceRateService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Business rules implemented here (see class Javadoc on
 * PriceRateRepository#findActiveForUpdate for the concurrency mechanism):
 *
 * Rule 1/2/3 - at most one ACTIVE price rate ever exists. Creating or
 * updating a rate to ACTIVE automatically deactivates whatever was
 * previously active, inside the same transaction, under a pessimistic lock
 * on the previously-active row so two concurrent "activate" calls for two
 * different rates cannot both succeed (see class Javadoc above).
 *
 * Rule 4 - deactivating the sole active rate (ACTIVE -> INACTIVE with no
 * other rate to take its place) is REJECTED with a BusinessException. This
 * project treats "there must always be a rate in effect" as the safer
 * default business state; flip the check in deactivateCurrentActiveGuard()
 * if the desired behavior is instead to allow zero active rates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceRateServiceImpl implements PriceRateService {

    private final PriceRateRepository priceRateRepository;
    private final PriceRateMapper priceRateMapper;
    private final DailyRouteRepository dailyRouteRepository;

    @Override
    @Transactional
    public PriceRateResponse createPriceRate(PriceRateCreateRequest request) {
        String actor = SecurityUtil.getCurrentUsername();

        PriceRate priceRate = PriceRate.builder()
                .price(request.getPrice())
                .status(PriceRateStatus.INACTIVE) // set below via activation path if requested ACTIVE
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        if (request.getStatus() == PriceRateStatus.ACTIVE) {
            deactivateCurrentlyActive(actor);
            priceRate.setStatus(PriceRateStatus.ACTIVE);
        }

        PriceRate saved = priceRateRepository.save(priceRate);
        log.info("Price rate created: id={}, status={}, by={}", saved.getId(), saved.getStatus(), actor);
        return priceRateMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceRateResponse getPriceRate(Long id) {
        return priceRateMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PriceRateResponse> getPriceRates(PriceRateStatus statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return priceRateRepository.findByStatus(statusFilter, pageable).map(priceRateMapper::toResponse);
        }
        return priceRateRepository.findAll(pageable).map(priceRateMapper::toResponse);
    }

    @Override
    @Transactional
    public PriceRateResponse updatePriceRate(Long id, PriceRateUpdateRequest request) {
        PriceRate priceRate = findOrThrow(id);
        String actor = SecurityUtil.getCurrentUsername();

        boolean becomingActive = request.getStatus() == PriceRateStatus.ACTIVE
                && priceRate.getStatus() != PriceRateStatus.ACTIVE;
        boolean becomingInactive = request.getStatus() == PriceRateStatus.INACTIVE
                && priceRate.getStatus() == PriceRateStatus.ACTIVE;

        if (becomingInactive) {
            // Rule 4: block deactivating the sole active rate.
            throw new BusinessException(
                    "Cannot deactivate the only active price rate. Activate a replacement rate instead.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }

        if (becomingActive) {
            deactivateCurrentlyActive(actor);
        }

        priceRate.setPrice(request.getPrice());
        priceRate.setStatus(request.getStatus());
        priceRate.setModifiedBy(actor);

        PriceRate saved = priceRateRepository.save(priceRate);
        log.info("Price rate updated: id={}, status={}, by={}", saved.getId(), saved.getStatus(), actor);
        return priceRateMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePriceRate(Long id) {
        PriceRate priceRate = findOrThrow(id);
        if (priceRate.getStatus() == PriceRateStatus.ACTIVE) {
            throw new BusinessException(
                    "Cannot delete the currently active price rate. Activate a replacement rate first.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }
        // Historical DailyRoute rows FK-reference specific PriceRate rows so
        // their billed rate remains correct even after that rate is
        // deactivated (see DailyRoute entity Javadoc) - which only works if
        // the referenced PriceRate is never actually deleted out from under
        // them.
        if (dailyRouteRepository.existsByPriceRateIdAndDeletedFalse(id)) {
            throw new BusinessException(
                    "Cannot delete a price rate that is referenced by existing daily route records.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }
        priceRateRepository.delete(priceRate);
        log.info("Price rate deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public PriceRateResponse getActivePriceRate() {
        PriceRate active = priceRateRepository.findByStatus(PriceRateStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active price rate is currently set", ErrorCodeConstants.PRICE_RATE_NOT_FOUND));
        return priceRateMapper.toResponse(active);
    }

    /**
     * Locks and deactivates whatever price rate is currently ACTIVE, if any.
     * Locking here (rather than a plain SELECT) is what prevents two
     * concurrent "activate rate X" / "activate rate Y" transactions from
     * both reading "no active rate yet" and both succeeding.
     */
    private void deactivateCurrentlyActive(String actor) {
        Optional<PriceRate> currentActive = priceRateRepository.findActiveForUpdate();
        currentActive.ifPresent(rate -> {
            rate.setStatus(PriceRateStatus.INACTIVE);
            rate.setModifiedBy(actor);
            priceRateRepository.save(rate);
            log.info("Price rate auto-deactivated: id={}, replacedBy activation, by={}", rate.getId(), actor);
        });
    }

    private PriceRate findOrThrow(Long id) {
        return priceRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Price rate not found with id: " + id, ErrorCodeConstants.PRICE_RATE_NOT_FOUND));
    }
}
