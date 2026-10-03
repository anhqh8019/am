CREATE TABLE service_tariffs (
    id BIGINT IDENTITY PRIMARY KEY,
    service_code VARCHAR(30) NOT NULL,
    building_id BIGINT NULL REFERENCES buildings(id),
    unit_price DECIMAL(18,2) NOT NULL,
    calculation_type VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_service_tariff_price CHECK (unit_price >= 0),
    CONSTRAINT ck_service_tariff_calculation CHECK (calculation_type IN ('BY_AREA','FIXED','PER_UNIT')),
    CONSTRAINT ck_service_tariff_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_service_tariffs_lookup
    ON service_tariffs(service_code, building_id, active, effective_from, effective_to);

