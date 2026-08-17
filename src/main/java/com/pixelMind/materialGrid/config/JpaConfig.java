package com.pixelMind.materialGrid.config;

import org.springframework.context.annotation.Configuration;

/**
 * Explicit-service-level auditing (addedBy/modifiedBy populated in
 * PriceRateServiceImpl / UserServiceImpl via SecurityUtil.getCurrentUsername())
 * is used in this project instead of Spring Data JPA's @CreatedBy/@LastModifiedBy
 * auditing infrastructure. Rationale: JPA auditing resolves the current
 * principal through an AuditorAware bean at flush time, which is one more
 * layer of indirection between "who is authenticated" and "what got
 * written" - for a security-sensitive audit trail, an explicit, readable
 * service-layer assignment is easier to reason about, test, and code-review
 * than an implicit entity-listener callback. @CreatedDate/@LastModifiedDate
 * timestamping is likewise handled explicitly in @PrePersist/@PreUpdate on
 * the entities for the same reason. This class is retained as an explicit
 * marker/config extension point (e.g. custom Jackson Hibernate module,
 * repository factory customization) even though JPA auditing itself is not
 * enabled.
 */
@Configuration
public class JpaConfig {
}
