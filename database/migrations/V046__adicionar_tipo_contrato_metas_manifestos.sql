IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'contract_type') IS NULL
BEGIN
    ALTER TABLE acesso.manifestos_cost_goals
        ADD contract_type NVARCHAR(100) NULL;
END
GO

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'contract_type_key') IS NULL
BEGIN
    ALTER TABLE acesso.manifestos_cost_goals
        ADD contract_type_key NVARCHAR(100) NULL;
END
GO

UPDATE acesso.manifestos_cost_goals
SET contract_type = N'Geral',
    contract_type_key = N'geral'
WHERE contract_type IS NULL;
GO

UPDATE acesso.manifestos_cost_goals
SET contract_type_key = N'geral'
WHERE contract_type_key IS NULL;
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
      AND name = N'contract_type_key'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE acesso.manifestos_cost_goals
        ALTER COLUMN contract_type_key NVARCHAR(100) NOT NULL;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_branch_period'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    DROP INDEX UX_manifestos_cost_goals_branch_period
        ON acesso.manifestos_cost_goals;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_global_period'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    DROP INDEX UX_manifestos_cost_goals_global_period
        ON acesso.manifestos_cost_goals;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_branch_period_contract'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_manifestos_cost_goals_branch_period_contract
        ON acesso.manifestos_cost_goals (branch_id, year_month, contract_type_key)
        WHERE branch_id IS NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_global_period_contract'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_manifestos_cost_goals_global_period_contract
        ON acesso.manifestos_cost_goals (year_month, contract_type_key)
        WHERE branch_id IS NULL;
END
GO
