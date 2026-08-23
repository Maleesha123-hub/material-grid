-- material_grid.routes definition

CREATE TABLE `routes`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT,
    `route_code`     varchar(20)    NOT NULL,
    `start_location` varchar(150)   NOT NULL,
    `end_location`   varchar(150)   NOT NULL,
    `km`             decimal(10, 2) NOT NULL,
    `created_by`     varchar(50)             DEFAULT NULL,
    `created_date`   datetime(6) NOT NULL,
    `modified_by`    varchar(50)             DEFAULT NULL,
    `modified_date`  datetime(6) NOT NULL,
    `version`        bigint         NOT NULL DEFAULT '0',
    `active`         bit(1)         NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_routes_route_code` (`route_code`),
    CONSTRAINT `chk_routes_km_positive` CHECK ((`km` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;