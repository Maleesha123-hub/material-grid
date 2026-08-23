-- material_grid.user_sessions definition

CREATE TABLE `user_sessions`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT,
    `user_id`          bigint       NOT NULL,
    `session_token`    varchar(100) NOT NULL,
    `status`           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    `login_date`       datetime(6) NOT NULL,
    `last_access_date` datetime(6) NOT NULL,
    `logout_date`      datetime(6) DEFAULT NULL,
    `version`          bigint       NOT NULL DEFAULT '0',
    `active_marker`    bigint GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'ACTIVE') then `user_id` else NULL end)) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sessions_token` (`session_token`),
    UNIQUE KEY `uk_user_sessions_active_per_user` (`active_marker`),
    KEY                `idx_user_sessions_user_id` (`user_id`),
    KEY                `idx_user_sessions_status` (`status`),
    CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;