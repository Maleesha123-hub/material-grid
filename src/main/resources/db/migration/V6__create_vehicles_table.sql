-- material_grid.vehicles definition

CREATE TABLE `vehicles`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT,
    `vehicle_number` varchar(20)    NOT NULL,
    `capacity`       decimal(10, 2) NOT NULL,
    `created_by`     varchar(50)             DEFAULT NULL,
    `created_date`   datetime(6) NOT NULL,
    `modified_by`    varchar(50)             DEFAULT NULL,
    `modified_date`  datetime(6) NOT NULL,
    `version`        bigint         NOT NULL DEFAULT '0',
    `active`         tinyint(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicles_vehicle_number` (`vehicle_number`),
    CONSTRAINT `chk_vehicles_capacity_positive` CHECK ((`capacity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;