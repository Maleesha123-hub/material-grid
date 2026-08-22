CREATE TABLE routes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_code      VARCHAR(20)   NOT NULL,
    start_location  VARCHAR(150)  NOT NULL,
    end_location    VARCHAR(150)  NOT NULL,
    km              DECIMAL(10,2) NOT NULL,
    created_by      VARCHAR(50),
    created_date    DATETIME(6)   NOT NULL,
    modified_by     VARCHAR(50),
    modified_date   DATETIME(6)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,

    CONSTRAINT uk_routes_route_code UNIQUE (route_code),
    CONSTRAINT chk_routes_km_positive CHECK (km > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- route_code is already unique-indexed above, which also covers lookup by code.
