-- material_grid.vehicle_expenses definition

CREATE TABLE `vehicle_expenses`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `expense_date`  date           NOT NULL,
    `expenses`      decimal(19, 4) NOT NULL,
    `vehicle_id`    bigint         NOT NULL,
    `deleted`       tinyint(1) NOT NULL DEFAULT '0',
    `created_by`    varchar(50) DEFAULT NULL,
    `created_date`  datetime(6) NOT NULL,
    `modified_by`   varchar(50) DEFAULT NULL,
    `modified_date` datetime(6) NOT NULL,
    `active`        bit(1)         NOT NULL,
    `version`       bigint         NOT NULL,
    PRIMARY KEY (`id`),
    KEY             `idx_vehicle_expenses_vehicle_date` (`vehicle_id`,`expense_date`),
    KEY             `idx_vehicle_expenses_deleted` (`deleted`),
    CONSTRAINT `fk_vehicle_expenses_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`),
    CONSTRAINT `chk_vehicle_expenses_positive` CHECK ((`expenses` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;