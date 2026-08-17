package com.pixelMind.materialGrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class Route extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idroute")
    private Long id;

    // Assigned server-side by CodeGeneratorService before the first save -
    // never accepted from client input. See CodeGeneratorService for the
    // concurrency-safe generation strategy.
    @Column(name = "route_code", nullable = false, unique = true, length = 20)
    private String routeCode;

    @Column(name = "start_location", nullable = false, length = 150)
    private String startLocation;

    @Column(name = "end_location", nullable = false, length = 150)
    private String endLocation;

    @Column(name = "km", nullable = false)
    private double km;

    private BigDecimal price;
}
