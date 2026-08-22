CREATE TABLE daily_routes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_date      DATE          NOT NULL,
    vehicle_id      BIGINT        NOT NULL,
    price_rate_id   BIGINT        NOT NULL,
    route_id        BIGINT        NOT NULL,
    amount          DECIMAL(19,4) NOT NULL,
    check_by        VARCHAR(100)  NOT NULL,
    bill_number     VARCHAR(100)  NOT NULL,
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,
    created_by      VARCHAR(50),
    created_date    DATETIME(6)   NOT NULL,
    modified_by     VARCHAR(50),
    modified_date   DATETIME(6)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_daily_routes_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_daily_routes_price_rate FOREIGN KEY (price_rate_id) REFERENCES price_rates (id),
    CONSTRAINT fk_daily_routes_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT chk_daily_routes_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Every filter exposed by GET /api/v1/daily-routes (date, vehicleId,
-- routeId, priceRateId) gets its own index; these are narrow, frequently
-- queried, low-cardinality-per-value columns on what will be the largest
-- table in the schema (one row per vehicle per route per day), so the
-- read-path benefit clearly outweighs the marginal write cost.
CREATE INDEX idx_daily_routes_date ON daily_routes (route_date);
CREATE INDEX idx_daily_routes_vehicle ON daily_routes (vehicle_id);
CREATE INDEX idx_daily_routes_route ON daily_routes (route_id);
CREATE INDEX idx_daily_routes_price_rate ON daily_routes (price_rate_id);
CREATE INDEX idx_daily_routes_deleted ON daily_routes (deleted);
