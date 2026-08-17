package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.CodeSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, String> {

    /**
     * Pessimistic write lock on a single named counter row. This is the
     * entire concurrency mechanism for code generation: while one
     * transaction holds this lock, every other concurrent caller for the
     * SAME sequence blocks until it commits and releases the row, so
     * "read current value, increment, write back" can never interleave
     * between two transactions and hand out the same number twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodeSequence c where c.sequenceName = :name")
    Optional<CodeSequence> findByNameForUpdate(@Param("name") String name);
}
