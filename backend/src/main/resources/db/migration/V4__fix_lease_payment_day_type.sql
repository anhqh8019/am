-- Hibernate maps Java int to SQL Server INT. V3 initially used TINYINT,
-- causing schema validation to stop backend startup. The V3 column-level
-- CHECK constraint has a SQL Server generated name, so resolve it safely.
DECLARE @check_name SYSNAME;
DECLARE @default_name SYSNAME;
DECLARE @sql NVARCHAR(MAX);

SELECT @check_name = cc.name
FROM sys.check_constraints cc
JOIN sys.columns c
  ON c.object_id = cc.parent_object_id
 AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID(N'dbo.lease_contracts')
  AND c.name = N'payment_day';

IF @check_name IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE dbo.lease_contracts DROP CONSTRAINT ' + QUOTENAME(@check_name) + N';';
    EXEC sys.sp_executesql @sql;
END;

SELECT @default_name = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c
  ON c.object_id = dc.parent_object_id
 AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID(N'dbo.lease_contracts')
  AND c.name = N'payment_day';

IF @default_name IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE dbo.lease_contracts DROP CONSTRAINT ' + QUOTENAME(@default_name) + N';';
    EXEC sys.sp_executesql @sql;
END;

ALTER TABLE dbo.lease_contracts ALTER COLUMN payment_day INT NOT NULL;
ALTER TABLE dbo.lease_contracts
    ADD CONSTRAINT df_lease_contracts_payment_day DEFAULT 5 FOR payment_day;
ALTER TABLE dbo.lease_contracts
    ADD CONSTRAINT ck_lease_contracts_payment_day CHECK (payment_day BETWEEN 1 AND 28);
