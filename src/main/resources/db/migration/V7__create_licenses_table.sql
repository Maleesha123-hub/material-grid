-- material_grid.licenses definition

CREATE TABLE `licenses`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `license_code`  varchar(20)    NOT NULL,
    `start_date`    date           NOT NULL,
    `end_date`      date           NOT NULL,
    `price`         decimal(19, 4) NOT NULL,
    `created_by`    varchar(50)             DEFAULT NULL,
    `created_date`  datetime(6) NOT NULL,
    `modified_by`   varchar(50)             DEFAULT NULL,
    `modified_date` datetime(6) NOT NULL,
    `version`       bigint         NOT NULL DEFAULT '0',
    `active`        bit(1)         NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_licenses_license_code` (`license_code`),
    CONSTRAINT `chk_licenses_date_range` CHECK ((`end_date` >= `start_date`)),
    CONSTRAINT `chk_licenses_price_positive` CHECK ((`price` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;