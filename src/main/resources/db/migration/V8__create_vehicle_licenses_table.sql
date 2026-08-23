-- material_grid.vehicle_licenses definition

CREATE TABLE `vehicle_licenses`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT,
    `vehicle_id`      bigint      NOT NULL,
    `license_id`      bigint      NOT NULL,
    `assignment_date` date        NOT NULL,
    `status`          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    `created_by`      varchar(50)          DEFAULT NULL,
    `created_date`    datetime(6) NOT NULL,
    `modified_by`     varchar(50)          DEFAULT NULL,
    `modified_date`   datetime(6) NOT NULL,
    `version`         bigint      NOT NULL DEFAULT '0',
    `active`          bit(1)      NOT NULL,
    PRIMARY KEY (`id`),
    KEY               `idx_vehicle_licenses_vehicle_status` (`vehicle_id`,`status`),
    KEY               `idx_vehicle_licenses_license_status` (`license_id`,`status`),
    CONSTRAINT `fk_vehicle_licenses_license` FOREIGN KEY (`license_id`) REFERENCES `licenses` (`id`),
    CONSTRAINT `fk_vehicle_licenses_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;