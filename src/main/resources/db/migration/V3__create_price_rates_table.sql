-- material_grid.price_rates definition

CREATE TABLE `price_rates`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `price`         decimal(19, 4) NOT NULL,
    `status`        varchar(20)    NOT NULL DEFAULT 'INACTIVE',
    `added_by`      varchar(50)    NOT NULL,
    `added_date`    datetime(6) NOT NULL,
    `modified_by`   varchar(50)             DEFAULT NULL,
    `modified_date` datetime(6) NOT NULL,
    `version`       bigint         NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY             `idx_price_rates_status` (`status`),
    CONSTRAINT `chk_price_rates_price_positive` CHECK ((`price` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;