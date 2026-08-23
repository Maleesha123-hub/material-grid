-- material_grid.users definition

CREATE TABLE `users`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `username`      varchar(50)  NOT NULL,
    `password`      varchar(255) NOT NULL,
    `status`        varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    `created_by`    varchar(50)           DEFAULT NULL,
    `created_date`  datetime(6) NOT NULL,
    `modified_by`   varchar(50)           DEFAULT NULL,
    `modified_date` datetime(6) NOT NULL,
    `version`       bigint       NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    KEY             `idx_users_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;