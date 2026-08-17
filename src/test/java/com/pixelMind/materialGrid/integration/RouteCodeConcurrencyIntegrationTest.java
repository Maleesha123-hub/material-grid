package com.pixelMind.materialGrid.integration;

import com.pixelMind.materialGrid.dto.request.RouteCreateRequest;
import com.pixelMind.materialGrid.repository.RouteRepository;
import com.pixelMind.materialGrid.service.RouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires many concurrent route-creation requests and asserts every generated
 * route code is unique - proving CodeGeneratorService's pessimistic locking
 * (not just sequential unit tests) holds under real concurrent load against
 * a real MySQL instance, and that no {@code count()+1}-style duplicate ever
 * slips through.
 */
class RouteCodeConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RouteService routeService;
    @Autowired
    private RouteRepository routeRepository;

    @Test
    @WithMockUser(username = "system")
    void concurrentRouteCreation_neverGeneratesDuplicateCodes() throws InterruptedException {
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    routeService.createRoute(new RouteCreateRequest("A", "B", new BigDecimal("5.00")));
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(20, TimeUnit.SECONDS);
        pool.shutdown();

        List<String> codes = routeRepository.findAll().stream()
                .map(r -> r.getRouteCode())
                .collect(Collectors.toList());

        assertThat(codes).hasSize(threadCount);
        assertThat(Set.copyOf(codes)).hasSize(threadCount); // no duplicates
    }
}
