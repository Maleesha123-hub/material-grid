-- material_grid.code_sequences definition

CREATE TABLE `code_sequences`
(
    `sequence_name` varchar(50) NOT NULL,
    `next_value`    bigint      NOT NULL,
    PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;