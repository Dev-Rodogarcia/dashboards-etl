SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.kpi_goals', N'U') IS NOT NULL
BEGIN
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

    IF COL_LENGTH(N'acesso.kpi_goals', N'created_at') IS NULL
    BEGIN
        ALTER TABLE acesso.kpi_goals
        ADD created_at DATETIME2(0) NOT NULL CONSTRAINT DF_kpi_goals_created_at DEFAULT SYSUTCDATETIME();
    END;

    IF COLUMNPROPERTY(OBJECT_ID(N'acesso.kpi_goals', N'U'), N'branch_id', 'AllowsNull') = 0
    BEGIN
        ALTER TABLE acesso.kpi_goals
        ALTER COLUMN branch_id NVARCHAR(120) NULL;
    END;

    UPDATE acesso.kpi_goals
    SET branch_id = NULL
    WHERE UPPER(branch_id) = 'GLOBAL';

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = N'UQ_kpi_goals_branch_indicator'
          AND parent_object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
    )
    BEGIN
        ALTER TABLE acesso.kpi_goals
        ADD CONSTRAINT UQ_kpi_goals_branch_indicator UNIQUE (branch_id, indicator_key);
    END;
END;
GO

IF OBJECT_ID(N'acesso.kpi_goals_history', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.kpi_goals_history (
        id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        branch_id          NVARCHAR(120)        NULL,
        indicator_key      VARCHAR(60)          NOT NULL,
        old_value          DECIMAL(9,3)         NULL,
        new_value          DECIMAL(9,3)         NULL,
        updated_by_user_id BIGINT               NULL REFERENCES acesso.usuarios(id),
        updated_at         DATETIME2(0)         NOT NULL CONSTRAINT DF_kpi_goals_history_updated_at DEFAULT SYSUTCDATETIME(),
        action             VARCHAR(40)          NOT NULL,
        CONSTRAINT CK_kpi_goals_history_indicator_key CHECK (
            indicator_key IN (
                'delivery_performance',
                'collector_usage',
                'cargo_cubage',
                'cargo_indemnity',
                'cutoff_time'
            )
        ),
        CONSTRAINT CK_kpi_goals_history_action CHECK (
            action IN ('GLOBAL_UPDATE', 'BRANCH_UPDATE', 'BRANCH_OVERRIDE_REMOVED')
        )
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_kpi_goals_history_branch_updated_at'
      AND object_id = OBJECT_ID(N'acesso.kpi_goals_history', N'U')
)
BEGIN
    CREATE INDEX IX_kpi_goals_history_branch_updated_at
    ON acesso.kpi_goals_history (branch_id, updated_at DESC);
END;
GO
