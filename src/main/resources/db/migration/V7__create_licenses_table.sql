CREATE TABLE licenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_code    VARCHAR(20)   NOT NULL,
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,
    price           DECIMAL(19,4) NOT NULL,
    created_by      VARCHAR(50),
    created_date    DATETIME(6)   NOT NULL,
    modified_by     VARCHAR(50),
    modified_date   DATETIME(6)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,

    CONSTRAINT uk_licenses_license_code UNIQUE (license_code),
    CONSTRAINT chk_licenses_price_positive CHECK (price > 0),
    CONSTRAINT chk_licenses_date_range CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
