package com.pixelMind.materialGrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class Vehicle extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idvehicle")
    private Long id;

    // User-supplied, unlike routeCode/licenseCode - validated for format and
    // uniqueness (application check + DB unique constraint, same
    // belt-and-braces pattern used for User.username).
    @Column(name = "vehicle_number", nullable = false, unique = true, length = 20)
    private String vehicleNumber;

    // Load capacity, e.g. tons or cubic meters depending on fleet type -
    // BigDecimal since fractional capacities are meaningful (e.g. 2.5 tons)
    // and this value may factor into future cost/billing calculations
    // where double's binary rounding would be inappropriate.
    @Column(name = "capacity", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacity;
}
