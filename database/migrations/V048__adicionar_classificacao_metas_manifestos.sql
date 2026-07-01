IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'classification_key') IS NULL
BEGIN
    ALTER TABLE acesso.manifestos_cost_goals
        ADD classification_key VARCHAR(120) NULL;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_branch_period_contract'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    DROP INDEX UX_manifestos_cost_goals_branch_period_contract
        ON acesso.manifestos_cost_goals;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_global_period_contract'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    DROP INDEX UX_manifestos_cost_goals_global_period_contract
        ON acesso.manifestos_cost_goals;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_branch_period_contract_classification'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_manifestos_cost_goals_branch_period_contract_classification
        ON acesso.manifestos_cost_goals (branch_id, year_month, contract_type_key, classification_key)
        WHERE branch_id IS NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_global_period_contract_classification'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_manifestos_cost_goals_global_period_contract_classification
        ON acesso.manifestos_cost_goals (year_month, contract_type_key, classification_key)
        WHERE branch_id IS NULL;
END
GO
