CREATE TABLE buildings (
    id BIGINT IDENTITY PRIMARY KEY,
    code NVARCHAR(20) NOT NULL UNIQUE,
    name NVARCHAR(150) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE floors (
    id BIGINT IDENTITY PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES buildings(id),
    floor_number INT NOT NULL,
    name NVARCHAR(50),
    CONSTRAINT uq_floor UNIQUE(building_id, floor_number)
);

CREATE TABLE units (
    id BIGINT IDENTITY PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES buildings(id),
    floor_id BIGINT NULL REFERENCES floors(id),
    code NVARCHAR(30) NOT NULL,
    unit_type VARCHAR(20) NOT NULL CHECK (unit_type IN ('APARTMENT','COMMERCIAL')),
    area_m2 DECIMAL(12,2) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    row_version ROWVERSION,
    CONSTRAINT uq_unit UNIQUE(building_id, code)
);

CREATE TABLE customers (
    id BIGINT IDENTITY PRIMARY KEY,
    customer_code NVARCHAR(30) NOT NULL UNIQUE,
    customer_type VARCHAR(20) NOT NULL CHECK (customer_type IN ('INDIVIDUAL','ORGANIZATION')),
    full_name NVARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    tax_code VARCHAR(30),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE unit_occupancies (
    id BIGINT IDENTITY PRIMARY KEY,
    unit_id BIGINT NOT NULL REFERENCES units(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    occupancy_role VARCHAR(20) NOT NULL CHECK (occupancy_role IN ('OWNER','TENANT','REPRESENTATIVE')),
    start_date DATE NOT NULL,
    end_date DATE NULL,
    is_primary BIT NOT NULL DEFAULT 0
);

CREATE TABLE service_types (
    id BIGINT IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name NVARCHAR(150) NOT NULL,
    billing_method VARCHAR(30) NOT NULL,
    active BIT NOT NULL DEFAULT 1
);

CREATE TABLE billing_periods (
    id BIGINT IDENTITY PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
);

CREATE TABLE receivables (
    id BIGINT IDENTITY PRIMARY KEY,
    reference_code VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    unit_id BIGINT NULL REFERENCES units(id),
    billing_period_id BIGINT NOT NULL REFERENCES billing_periods(id),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    row_version ROWVERSION
);

CREATE TABLE receivable_items (
    id BIGINT IDENTITY PRIMARY KEY,
    receivable_id BIGINT NOT NULL REFERENCES receivables(id),
    service_type_id BIGINT NOT NULL REFERENCES service_types(id),
    description NVARCHAR(300) NOT NULL,
    quantity DECIMAL(18,3) NOT NULL DEFAULT 1,
    unit_price DECIMAL(18,2) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    service_period_start DATE NULL,
    service_period_end DATE NULL
);

CREATE INDEX ix_receivables_customer_status ON receivables(customer_id, status);
CREATE INDEX ix_receivables_due_date ON receivables(due_date, status);

INSERT INTO buildings(code, name) VALUES ('A', N'Tòa A'), ('B', N'Tòa B');
INSERT INTO service_types(code, name, billing_method) VALUES
('SERVICE_FEE', N'Phí dịch vụ', 'BY_AREA'),
('PARKING_MONTHLY', N'Phí gửi xe cư dân', 'FIXED'),
('PARKING_VISITOR', N'Phí gửi xe vãng lai', 'TRANSACTION'),
('COMMERCIAL_RENT', N'Tiền thuê mặt bằng', 'CONTRACT'),
('COMMERCIAL_ELECTRIC', N'Tiền điện mặt bằng', 'METER');
