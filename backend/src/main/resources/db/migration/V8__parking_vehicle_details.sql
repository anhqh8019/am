IF COL_LENGTH('dbo.unit_service_assignments', 'license_plate') IS NULL
    EXEC(N'ALTER TABLE dbo.unit_service_assignments
        ADD license_plate VARCHAR(20) NULL');

IF COL_LENGTH('dbo.unit_service_assignments', 'electric_vehicle') IS NULL
    EXEC(N'ALTER TABLE dbo.unit_service_assignments
        ADD electric_vehicle BIT NOT NULL
        CONSTRAINT df_unit_service_electric_vehicle DEFAULT 0');

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'ux_active_parking_license_plate'
      AND object_id = OBJECT_ID('dbo.unit_service_assignments')
)
    EXEC(N'CREATE UNIQUE INDEX ux_active_parking_license_plate
        ON dbo.unit_service_assignments(license_plate)
        WHERE active = 1 AND license_plate IS NOT NULL');
