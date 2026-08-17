package com.pixelMind.materialGrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a historical financial record. Deleting one destroys audit
 * trail / accounting history, so this entity is soft-deleted: the "delete"
 * API sets {@code deleted = true} rather than issuing a SQL DELETE. All
 * repository read paths filter on {@code deleted = false} by default. See
 * VehicleExpenseServiceImpl for the rationale.
 */
@Entity
@Table(name = "vehicle_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class VehicleExpense extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Column(name = "expenses", nullable = false, precision = 19, scale = 4)
    private BigDecimal expenses;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_idvehicle", nullable = false)
    private Vehicle vehicle;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
