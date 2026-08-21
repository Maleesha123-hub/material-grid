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
 * Represents a historical operational + financial record - soft-deleted,
 * same rationale as VehicleExpense. {@code priceRate} is a normal FK to the
 * specific PriceRate row used at creation/last-update time. {@code amount}
 * is computed server-side and persisted, never trusted from client input.
 * {@code checkBy} records who physically checked/verified the route.
 *
 * {@code loadCount}: ADDED for the Daily Route PDF report feature. This is
 * currently an UNRESOLVED GAP flagged during that feature's implementation -
 * no existing part of the project populated this value before now. It is
 * nullable and NOT yet threaded through DailyRouteCreateRequest,
 * DailyRouteUpdateRequest, DailyRouteServiceImpl, or the Excel importer;
 * until that follow-up work is done, it will read as null (reported as 0)
 * for every route. See the Daily Route PDF feature's architectural notes.
 */
@Entity
@Table(name = "daily_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class DailyRoute extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_rate_id", nullable = false)
    private PriceRate priceRate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "check_by", nullable = false, length = 100)
    private String checkBy;

    // See class Javadoc - not yet wired into CRUD/Excel import.
//    @Column(name = "load_count")
//    private Integer loadCount;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}