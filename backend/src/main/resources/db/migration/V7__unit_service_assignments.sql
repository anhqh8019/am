CREATE TABLE unit_service_assignments (
    id BIGINT IDENTITY PRIMARY KEY,
    unit_id BIGINT NOT NULL REFERENCES units(id),
    service_code VARCHAR(30) NOT NULL,
    service_name NVARCHAR(150) NOT NULL,
    variant NVARCHAR(100) NULL,
    charge_method VARCHAR(20) NOT NULL CHECK (charge_method IN ('BY_AREA','FIXED','PER_UNIT')),
    quantity DECIMAL(18,3) NOT NULL DEFAULT 1,
    unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    collection_mode VARCHAR(30) NOT NULL CHECK (collection_mode IN ('MANAGEMENT','PROVIDER')),
    mandatory BIT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_unit_service_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX ix_unit_services_active ON unit_service_assignments(unit_id, active, service_code);

INSERT INTO unit_service_assignments(
    unit_id, service_code, service_name, charge_method, quantity,
    unit_price, collection_mode, mandatory, active, start_date
)
SELECT
    u.id, 'SERVICE_FEE', N'Phí dịch vụ tòa nhà', 'BY_AREA',
    COALESCE(u.area_m2, 1), 0, 'MANAGEMENT', 1, 1, CAST(GETDATE() AS DATE)
FROM units u;

IF NOT EXISTS (SELECT 1 FROM service_types WHERE code='INTERNET')
    INSERT INTO service_types(code,name,billing_method) VALUES ('INTERNET',N'Internet','FIXED');
IF NOT EXISTS (SELECT 1 FROM service_types WHERE code='ELECTRICITY')
    INSERT INTO service_types(code,name,billing_method) VALUES ('ELECTRICITY',N'Điện','METER');
IF NOT EXISTS (SELECT 1 FROM service_types WHERE code='WATER')
    INSERT INTO service_types(code,name,billing_method) VALUES ('WATER',N'Nước','METER');
