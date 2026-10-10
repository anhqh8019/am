CREATE TABLE commercial_service_assignments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    commercial_space_id BIGINT NOT NULL,
    service_code VARCHAR(30) NOT NULL,
    service_name NVARCHAR(150) NOT NULL,
    pricing_method VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(20) NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_commercial_service_active DEFAULT 1,
    CONSTRAINT fk_commercial_service_space FOREIGN KEY (commercial_space_id) REFERENCES commercial_spaces(id),
    CONSTRAINT ck_commercial_service_method CHECK (pricing_method IN ('PER_M2','FIXED','PER_VEHICLE'))
);
GO
CREATE INDEX ix_commercial_service_space ON commercial_service_assignments(commercial_space_id,active);
GO
CREATE TABLE commercial_parking_vehicles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    plate_number NVARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    electric_vehicle BIT NOT NULL CONSTRAINT df_commercial_vehicle_electric DEFAULT 0,
    active BIT NOT NULL CONSTRAINT df_commercial_vehicle_active DEFAULT 1,
    CONSTRAINT fk_commercial_vehicle_assignment FOREIGN KEY (assignment_id) REFERENCES commercial_service_assignments(id)
);
GO
CREATE UNIQUE INDEX uq_commercial_vehicle_plate_active ON commercial_parking_vehicles(plate_number) WHERE active=1;
