package com.pixelMind.materialGrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Backing store for the atomic, transaction-isolated code generator (see
 * util/CodeGeneratorService). One row per business code type
 * ("ROUTE_CODE", "LICENSE_CODE", ...), seeded by Flyway migration so the
 * generator only ever needs SELECT ... FOR UPDATE + UPDATE - never an
 * insert-or-update race.
 */
@Entity
@Table(name = "code_sequences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSequence {

    @Id
    @Column(name = "sequence_name", length = 50)
    private String sequenceName;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}
