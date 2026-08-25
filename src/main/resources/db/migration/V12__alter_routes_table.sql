ALTER TABLE daily_routes
    DROP FOREIGN KEY fk_daily_routes_price_rate;

ALTER TABLE daily_routes
    DROP COLUMN price_rate_id;

ALTER TABLE routes
    ADD COLUMN price DECIMAL(19,4) NOT NULL;

ALTER TABLE routes
    ADD CONSTRAINT chk_routes_price_positive
    CHECK (price > 0);

ALTER TABLE vehicle_licenses
    ADD COLUMN file_history_id BIGINT NULL,
    ADD CONSTRAINT fk_vehicle_licenses_file_history
        FOREIGN KEY (file_history_id) REFERENCES file_history (id);

CREATE INDEX idx_vehicle_licenses_file_history
    ON vehicle_licenses (file_history_id);

ALTER TABLE vehicle_licenses
    ADD CONSTRAINT uk_vehicle_licenses_vehicle_license_date
    UNIQUE (vehicle_id, license_id, assignment_date);