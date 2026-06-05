SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.kpi_goals', N'U') IS NULL
BEGIN
    THROW 52600, 'Tabela acesso.kpi_goals ausente. Aplique as migrations anteriores antes da V026.', 1;
END;
GO

IF COL_LENGTH(N'acesso.kpi_goals', N'competencia') IS NULL
BEGIN
    ALTER TABLE acesso.kpi_goals
    ADD competencia DATE NULL;
END;
GO

DECLARE @competenciaAtual DATE = DATEFROMPARTS(YEAR(SYSUTCDATETIME()), MONTH(SYSUTCDATETIME()), 1);

UPDATE acesso.kpi_goals
SET competencia = @competenciaAtual
WHERE competencia IS NULL;

IF COLUMNPROPERTY(OBJECT_ID(N'acesso.kpi_goals', N'U'), N'competencia', 'AllowsNull') = 1
BEGIN
    ALTER TABLE acesso.kpi_goals
    ALTER COLUMN competencia DATE NOT NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints
    WHERE name = N'DF_kpi_goals_competencia'
      AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
)
BEGIN
    ALTER TABLE acesso.kpi_goals
    ADD CONSTRAINT DF_kpi_goals_competencia
        DEFAULT (DATEFROMPARTS(YEAR(SYSUTCDATETIME()), MONTH(SYSUTCDATETIME()), 1))
    FOR competencia;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_kpi_goals_competencia_primeiro_dia'
      AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
)
BEGIN
    ALTER TABLE acesso.kpi_goals WITH CHECK
    ADD CONSTRAINT CK_kpi_goals_competencia_primeiro_dia
        CHECK (DAY(competencia) = 1);
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UQ_kpi_goals_branch_indicator'
      AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
)
BEGIN
    ALTER TABLE acesso.kpi_goals
    DROP CONSTRAINT UQ_kpi_goals_branch_indicator;
END;
GO

IF EXISTS (
    SELECT 1
    FROM acesso.kpi_goals
    GROUP BY branch_id, indicator_key, competencia
    HAVING COUNT(1) > 1
)
BEGIN
    THROW 52601, 'Duplicidade de metas por filial/indicador/competencia impede recriacao de UQ_kpi_goals_branch_indicator_competencia.', 1;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UQ_kpi_goals_branch_indicator_competencia'
      AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
)
BEGIN
    ALTER TABLE acesso.kpi_goals
    ADD CONSTRAINT UQ_kpi_goals_branch_indicator_competencia
        UNIQUE (branch_id, indicator_key, competencia);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_kpi_goals_competencia_branch'
      AND object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
)
BEGIN
    CREATE INDEX IX_kpi_goals_competencia_branch
    ON acesso.kpi_goals (competencia, branch_id, indicator_key);
END;
GO

IF OBJECT_ID(N'acesso.kpi_goals_history', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'acesso.kpi_goals_history', N'competencia') IS NULL
    BEGIN
        ALTER TABLE acesso.kpi_goals_history
        ADD competencia DATE NULL;
    END;
END;
GO

IF OBJECT_ID(N'acesso.kpi_goals_history', N'U') IS NOT NULL
BEGIN
    DECLARE @competenciaAtualHistorico DATE = DATEFROMPARTS(YEAR(SYSUTCDATETIME()), MONTH(SYSUTCDATETIME()), 1);

    UPDATE acesso.kpi_goals_history
    SET competencia = @competenciaAtualHistorico
    WHERE competencia IS NULL;

    IF COLUMNPROPERTY(OBJECT_ID(N'acesso.kpi_goals_history', N'U'), N'competencia', 'AllowsNull') = 1
    BEGIN
        ALTER TABLE acesso.kpi_goals_history
        ALTER COLUMN competencia DATE NOT NULL;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints
        WHERE name = N'DF_kpi_goals_history_competencia'
          AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals_history', N'U')
    )
    BEGIN
        ALTER TABLE acesso.kpi_goals_history
        ADD CONSTRAINT DF_kpi_goals_history_competencia
            DEFAULT (DATEFROMPARTS(YEAR(SYSUTCDATETIME()), MONTH(SYSUTCDATETIME()), 1))
        FOR competencia;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE name = N'CK_kpi_goals_history_competencia_primeiro_dia'
          AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals_history', N'U')
    )
    BEGIN
        ALTER TABLE acesso.kpi_goals_history WITH CHECK
        ADD CONSTRAINT CK_kpi_goals_history_competencia_primeiro_dia
            CHECK (DAY(competencia) = 1);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_kpi_goals_history_branch_competencia_updated_at'
          AND object_id = OBJECT_ID(N'acesso.kpi_goals_history', N'U')
    )
    BEGIN
        CREATE INDEX IX_kpi_goals_history_branch_competencia_updated_at
        ON acesso.kpi_goals_history (branch_id, competencia, updated_at DESC);
    END;
END;
GO
