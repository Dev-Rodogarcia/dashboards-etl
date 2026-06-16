SET NOCOUNT ON;

IF OBJECT_ID(N'acesso.manifestos_cost_goals', N'U') IS NULL
    THROW 50280, 'Tabela acesso.manifestos_cost_goals nao encontrada.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'branch_id') IS NULL
    THROW 50281, 'Coluna branch_id nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'year_month') IS NULL
    THROW 50282, 'Coluna year_month nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'cost_goal') IS NULL
    THROW 50283, 'Coluna cost_goal nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'created_at') IS NULL
    THROW 50284, 'Coluna created_at nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'updated_at') IS NULL
    THROW 50285, 'Coluna updated_at nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'updated_by_user_id') IS NULL
    THROW 50290, 'Coluna updated_by_user_id nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'contract_type') IS NULL
    THROW 50291, 'Coluna contract_type nao encontrada em acesso.manifestos_cost_goals.', 1;

IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'contract_type_key') IS NULL
    THROW 50292, 'Coluna contract_type_key nao encontrada em acesso.manifestos_cost_goals.', 1;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
      AND name = N'contract_type_key'
      AND is_nullable = 1
)
    THROW 50293, 'Coluna contract_type_key deve ser obrigatoria em acesso.manifestos_cost_goals.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_manifestos_cost_goals_year_month'
      AND parent_object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
    THROW 50286, 'Constraint de normalizacao da competencia nao encontrada.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_manifestos_cost_goals_cost_goal'
      AND parent_object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
    THROW 50287, 'Constraint de custo nao negativo nao encontrada.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_branch_period_contract'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
      AND is_unique = 1
      AND has_filter = 1
      AND REPLACE(REPLACE(filter_definition, N'[', N''), N']', N'')
          = N'(branch_id IS NOT NULL)'
)
    THROW 50288, 'Indice unico por filial, competencia e contrato nao encontrado ou invalido.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_global_period_contract'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
      AND is_unique = 1
      AND has_filter = 1
      AND REPLACE(REPLACE(filter_definition, N'[', N''), N']', N'')
          = N'(branch_id IS NULL)'
)
    THROW 50289, 'Indice unico global por competencia e contrato nao encontrado ou invalido.', 1;

PRINT 'Metas de custo de manifestos validadas com sucesso.';
