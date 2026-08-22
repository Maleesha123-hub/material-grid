CREATE TABLE file_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    file_type      VARCHAR(30)  NOT NULL,
    uploaded_by    VARCHAR(50)  NOT NULL,
    uploaded_date  DATETIME(6)  NOT NULL,
    deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT uk_file_history_name_type UNIQUE (file_name, file_type)
);

ALTER TABLE vehicles         ADD COLUMN file_history_id BIGINT NULL,
    ADD CONSTRAINT fk_vehicles_file_history FOREIGN KEY (file_history_id) REFERENCES file_history(id);
ALTER TABLE vehicle_expenses ADD COLUMN file_history_id BIGINT NULL,
    ADD CONSTRAINT fk_vehicle_expenses_file_history FOREIGN KEY (file_history_id) REFERENCES file_history(id);
ALTER TABLE daily_routes     ADD COLUMN file_history_id BIGINT NULL,
    ADD CONSTRAINT fk_daily_routes_file_history FOREIGN KEY (file_history_id) REFERENCES file_history(id);

CREATE INDEX idx_vehicles_file_history ON vehicles (file_history_id);
CREATE INDEX idx_vehicle_expenses_file_history ON vehicle_expenses (file_history_id);
CREATE INDEX idx_daily_routes_file_history ON daily_routes (file_history_id);