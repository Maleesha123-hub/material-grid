-- material_grid.daily_routes definition

CREATE TABLE `daily_routes`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `route_date`    date           NOT NULL,
    `vehicle_id`    bigint         NOT NULL,
    `price_rate_id` bigint         NOT NULL,
    `route_id`      bigint         NOT NULL,
    `amount`        decimal(19, 4) NOT NULL,
    `check_by`      varchar(100)   NOT NULL,
    `deleted`       tinyint(1) NOT NULL DEFAULT '0',
    `created_by`    varchar(50) DEFAULT NULL,
    `created_date`  datetime(6) NOT NULL,
    `modified_by`   varchar(50) DEFAULT NULL,
    `modified_date` datetime(6) NOT NULL,
    `active`        bit(1)         NOT NULL,
    `version`       bigint         NOT NULL,
    PRIMARY KEY (`id`),
    KEY             `idx_daily_routes_date` (`route_date`),
    KEY             `idx_daily_routes_vehicle` (`vehicle_id`),
    KEY             `idx_daily_routes_route` (`route_id`),
    KEY             `idx_daily_routes_price_rate` (`price_rate_id`),
    KEY             `idx_daily_routes_deleted` (`deleted`),
    CONSTRAINT `fk_daily_routes_price_rate` FOREIGN KEY (`price_rate_id`) REFERENCES `price_rates` (`id`),
    CONSTRAINT `fk_daily_routes_route` FOREIGN KEY (`route_id`) REFERENCES `routes` (`id`),
    CONSTRAINT `fk_daily_routes_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`),
    CONSTRAINT `chk_daily_routes_amount_positive` CHECK ((`amount` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;