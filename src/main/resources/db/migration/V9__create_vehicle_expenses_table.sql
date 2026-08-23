CREATE TABLE vehicle_expenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_date    DATE          NOT NULL,
    expenses        DECIMAL(19,4) NOT NULL,
    vehicle_id      BIGINT        NOT NULL,
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    created_by      VARCHAR(50),
    created_date    DATETIME(6)   NOT NULL,
    modified_by     VARCHAR(50),
    modified_date   DATETIME(6)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_vehicle_expenses_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT chk_vehicle_expenses_positive CHECK (expenses > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Supports "expenses for vehicle X" and date-range reporting, the two
-- filters the API explicitly exposes.
CREATE INDEX idx_vehicle_expenses_vehicle_date ON vehicle_expenses (vehicle_id, expense_date);
CREATE INDEX idx_vehicle_expenses_deleted ON vehicle_expenses (deleted);
