SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID(N'acesso.fretes_goals', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.fretes_goals (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_fretes_goals PRIMARY KEY,
        branch_id NVARCHAR(120) NULL,
        ano SMALLINT NOT NULL,
        mes TINYINT NOT NULL,
        meta_faturamento DECIMAL(18,2) NOT NULL CONSTRAINT DF_fretes_goals_meta_faturamento DEFAULT 0,
        meta_fretes INT NOT NULL CONSTRAINT DF_fretes_goals_meta_fretes DEFAULT 0,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_fretes_goals_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_fretes_goals_updated_at DEFAULT SYSUTCDATETIME(),
        updated_by_user_id BIGINT NULL,
        CONSTRAINT CK_fretes_goals_mes CHECK (mes BETWEEN 1 AND 12),
        CONSTRAINT CK_fretes_goals_meta_faturamento CHECK (meta_faturamento >= 0),
        CONSTRAINT CK_fretes_goals_meta_fretes CHECK (meta_fretes >= 0)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UX_fretes_goals_branch_period'
      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_fretes_goals_branch_period
    ON acesso.fretes_goals (branch_id, ano, mes)
    WHERE branch_id IS NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UX_fretes_goals_global_period'
      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_fretes_goals_global_period
    ON acesso.fretes_goals (ano, mes)
    WHERE branch_id IS NULL;
END
GO
