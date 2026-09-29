-- Use dynamic SQL because SQL Server compiles a batch before newly added
-- columns are visible to constraints and indexes later in the same batch.
IF COL_LENGTH(N'dbo.customers', N'birth_year') IS NULL
    EXEC(N'ALTER TABLE dbo.customers ADD birth_year INT NULL;');

IF COL_LENGTH(N'dbo.customers', N'gender') IS NULL
    EXEC(N'ALTER TABLE dbo.customers ADD gender VARCHAR(20) NULL;');

IF COL_LENGTH(N'dbo.customers', N'national_id') IS NULL
    EXEC(N'ALTER TABLE dbo.customers ADD national_id VARCHAR(20) NULL;');

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.customers')
      AND name = N'ck_customers_birth_year'
)
    EXEC(N'ALTER TABLE dbo.customers ADD CONSTRAINT ck_customers_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100);');

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.customers')
      AND name = N'ck_customers_gender'
)
    EXEC(N'ALTER TABLE dbo.customers ADD CONSTRAINT ck_customers_gender CHECK (gender IS NULL OR gender IN (''MALE'',''FEMALE'',''OTHER''));');

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.customers')
      AND name = N'ux_customers_national_id'
)
    EXEC(N'CREATE UNIQUE INDEX ux_customers_national_id ON dbo.customers(national_id) WHERE national_id IS NOT NULL;');
