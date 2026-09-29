CREATE TABLE commercial_spaces (
    id BIGINT IDENTITY PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES buildings(id),
    code NVARCHAR(30) NOT NULL,
    location NVARCHAR(200) NOT NULL,
    description NVARCHAR(500) NULL,
    total_area_m2 DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE','LEASED','INACTIVE')),
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    row_version ROWVERSION,
    CONSTRAINT uq_commercial_space UNIQUE(building_id, code)
);

CREATE TABLE commercial_tenants (
    id BIGINT IDENTITY PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE,
    name NVARCHAR(200) NOT NULL,
    tax_code VARCHAR(30) NULL,
    representative NVARCHAR(150) NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(150) NULL,
    billing_address NVARCHAR(300) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE lease_contracts (
    id BIGINT IDENTITY PRIMARY KEY,
    contract_number NVARCHAR(50) NOT NULL UNIQUE,
    commercial_space_id BIGINT NOT NULL REFERENCES commercial_spaces(id),
    tenant_id BIGINT NOT NULL REFERENCES commercial_tenants(id),
    leased_area_m2 DECIMAL(12,2) NOT NULL,
    unit_price_per_m2 DECIMAL(18,2) NOT NULL,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 10,
    deposit_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    payment_day TINYINT NOT NULL DEFAULT 5 CHECK (payment_day BETWEEN 1 AND 28),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','ACTIVE','EXPIRED','TERMINATED')),
    notes NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    row_version ROWVERSION,
    CONSTRAINT ck_lease_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_lease_area CHECK (leased_area_m2 > 0),
    CONSTRAINT ck_lease_price CHECK (unit_price_per_m2 >= 0)
);

CREATE TABLE electricity_meters (
    id BIGINT IDENTITY PRIMARY KEY,
    commercial_space_id BIGINT NOT NULL REFERENCES commercial_spaces(id),
    meter_code NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(200) NULL,
    default_unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE electricity_readings (
    id BIGINT IDENTITY PRIMARY KEY,
    meter_id BIGINT NOT NULL REFERENCES electricity_meters(id),
    period_code VARCHAR(20) NOT NULL,
    previous_index DECIMAL(18,3) NOT NULL,
    current_index DECIMAL(18,3) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    reading_date DATE NOT NULL,
    image_url NVARCHAR(500) NULL,
    note NVARCHAR(300) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_meter_period UNIQUE(meter_id, period_code),
    CONSTRAINT ck_meter_index CHECK (current_index >= previous_index)
);

CREATE INDEX ix_commercial_spaces_status ON commercial_spaces(status);
CREATE INDEX ix_lease_contracts_status_dates ON lease_contracts(status, start_date, end_date);
CREATE INDEX ix_electricity_readings_period ON electricity_readings(period_code);
