SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID(N'acesso.manifestos_cost_goals', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.manifestos_cost_goals (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_manifestos_cost_goals PRIMARY KEY,
        branch_id NVARCHAR(120) NULL,
        year_month DATE NOT NULL,
        cost_goal DECIMAL(18,2) NOT NULL,
        created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_manifestos_cost_goals_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_manifestos_cost_goals_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_manifestos_cost_goals_year_month
            CHECK (DAY(year_month) = 1),
        CONSTRAINT CK_manifestos_cost_goals_cost_goal
            CHECK (cost_goal >= 0)
    );
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_branch_period'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_manifestos_cost_goals_branch_period
        ON acesso.manifestos_cost_goals (branch_id, year_month)
        WHERE branch_id IS NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_manifestos_cost_goals_global_period'
      AND object_id = OBJECT_ID(N'acesso.manifestos_cost_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_manifestos_cost_goals_global_period
        ON acesso.manifestos_cost_goals (year_month)
        WHERE branch_id IS NULL;
END
GO
