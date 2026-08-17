package com.pixelMind.materialGrid.integration;

import com.pixelMind.materialGrid.dto.request.PriceRateCreateRequest;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import com.pixelMind.materialGrid.repository.PriceRateRepository;
import com.pixelMind.materialGrid.service.PriceRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the exact race condition described in the requirements: two
 * concurrent requests each trying to activate a *different* price rate.
 * Asserts the database never ends up with two ACTIVE rows, proving the
 * pessimistic-lock + unique-generated-column strategy holds under real
 * concurrent load (not just sequential unit tests).
 */
class PriceRateConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PriceRateService priceRateService;
    @Autowired
    private PriceRateRepository priceRateRepository;

    @Test
    void concurrentActivation_neverResultsInTwoActiveRates() throws InterruptedException {
        Long rateAId = priceRateService.createPriceRate(
                new PriceRateCreateRequest(new BigDecimal("100.00"), PriceRateStatus.INACTIVE)).getId();
        Long rateBId = priceRateService.createPriceRate(
                new PriceRateCreateRequest(new BigDecimal("200.00"), PriceRateStatus.INACTIVE)).getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        pool.submit(() -> {
            try {
                startLatch.await();
                priceRateService.updatePriceRate(rateAId,
                        new com.pixelMind.materialGrid.dto.request.PriceRateUpdateRequest(
                                new BigDecimal("100.00"), PriceRateStatus.ACTIVE));
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        pool.submit(() -> {
            try {
                startLatch.await();
                priceRateService.updatePriceRate(rateBId,
                        new com.pixelMind.materialGrid.dto.request.PriceRateUpdateRequest(
                                new BigDecimal("200.00"), PriceRateStatus.ACTIVE));
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        List<com.pixelMind.materialGrid.entity.PriceRate> activeRates =
                priceRateRepository.findAll().stream()
                        .filter(r -> r.getStatus() == PriceRateStatus.ACTIVE)
                        .toList();

        assertThat(activeRates).hasSize(1);
    }
}
