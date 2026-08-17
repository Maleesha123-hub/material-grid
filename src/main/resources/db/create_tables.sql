-- =============================================================================
-- PostgreSQL DDL Script for: Land, Vehicles, PriceRates, Routes, DailyRoutes
-- Database: material_grid_management
-- =============================================================================

-- 1. Land Table
CREATE TABLE IF NOT EXISTS land (
    idland BIGSERIAL PRIMARY KEY,
    land_code VARCHAR(255),
    land_name VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP,
    modified_by VARCHAR(50),
    modified_date TIMESTAMP
);

-- 2. Vehicles Table
CREATE TABLE IF NOT EXISTS vehicles (
    idvehicle BIGSERIAL PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    capacity NUMERIC(10, 2) NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP,
    modified_by VARCHAR(50),
    modified_date TIMESTAMP
);

-- 3. Price Rates Table
CREATE TABLE IF NOT EXISTS price_rates (
    idprice_rate BIGSERIAL PRIMARY KEY,
    price NUMERIC(19, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    updated_by VARCHAR(255),
    updated_date TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. Routes Table
CREATE TABLE IF NOT EXISTS routes (
    idroute BIGSERIAL PRIMARY KEY,
    route_code VARCHAR(20) NOT NULL UNIQUE,
    start_location VARCHAR(150) NOT NULL,
    end_location VARCHAR(150) NOT NULL,
    km NUMERIC(10, 2) NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP,
    modified_by VARCHAR(50),
    modified_date TIMESTAMP
);

-- 5. Daily Routes Table
CREATE TABLE IF NOT EXISTS daily_routes (
    id BIGSERIAL PRIMARY KEY,
    route_date DATE NOT NULL,
    vehicle_idvehicle BIGINT REFERENCES vehicles(idvehicle),
    price_rate_idprice_rate BIGINT REFERENCES price_rates(idprice_rate),
    price NUMERIC(19, 4),
    route_idroute BIGINT REFERENCES routes(idroute),
    km DOUBLE PRECISION,
    land_idland BIGINT REFERENCES land(idland),
    bill_number VARCHAR(255),
    cube DOUBLE PRECISION,
    daily_expenses NUMERIC(19, 4),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50),
    created_date TIMESTAMP,
    modified_by VARCHAR(50),
    modified_date TIMESTAMP
);
