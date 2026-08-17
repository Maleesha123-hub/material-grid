package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.PriceRate;
import com.pixelMind.materialGrid.entity.enums.PriceRateStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PriceRateRepository extends JpaRepository<PriceRate, Long> {

    Optional<PriceRate> findByStatus(PriceRateStatus status);

    Page<PriceRate> findByStatus(PriceRateStatus status, Pageable pageable);

    /**
     * Pessimistic write lock on the currently active price rate (if any).
     * Held for the duration of the activate/deactivate transaction so that
     * two concurrent "activate" requests for two different rates cannot both
     * succeed - the second waits for the first transaction's lock to release,
     * then re-reads a state where the first rate is already active.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PriceRate p where p.status = 'ACTIVE'")
    Optional<PriceRate> findActiveForUpdate();
}
