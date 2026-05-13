SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.kpi_goals', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.kpi_goals (
        id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        branch_id          NVARCHAR(120)        NULL,
        indicator_key      VARCHAR(60)          NOT NULL,
        goal_value         DECIMAL(9,3)         NOT NULL,
        created_at         DATETIME2(0)         NOT NULL CONSTRAINT DF_kpi_goals_created_at DEFAULT SYSUTCDATETIME(),
        updated_by_user_id BIGINT               NULL REFERENCES acesso.usuarios(id),
        updated_at         DATETIME2(0)         NOT NULL CONSTRAINT DF_kpi_goals_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_kpi_goals_branch_indicator UNIQUE (branch_id, indicator_key),
        CONSTRAINT CK_kpi_goals_indicator_key CHECK (
            indicator_key IN (
                'delivery_performance',
                'collector_usage',
                'cargo_cubage',
                'cargo_indemnity',
                'cutoff_time'
            )
        ),
        CONSTRAINT CK_kpi_goals_goal_value CHECK (goal_value >= 0 AND goal_value <= 100)
    );
END;
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

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_kpi_goals_branch_id'
      AND object_id = OBJECT_ID(N'acesso.kpi_goals', N'U')
)
BEGIN
    CREATE INDEX IX_kpi_goals_branch_id
    ON acesso.kpi_goals (branch_id);
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

IF NOT EXISTS (
    SELECT 1
    FROM acesso.permissoes
    WHERE chave_legado = 'can_manage_kpi_goals'
)
BEGIN
    INSERT INTO acesso.permissoes (
        chave,
        chave_legado,
        nome,
        descricao,
        recurso,
        acao,
        rota
    ) VALUES (
        'dashboard.can_manage_kpi_goals.read',
        'can_manage_kpi_goals',
        N'Gerenciar metas de indicadores',
        N'Criar e alterar metas dos Indicadores de Gestão por filial',
        'can_manage_kpi_goals',
        'read',
        '/indicadores-gestao-a-vista'
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM acesso.permissoes
    WHERE chave_legado = 'can_manage_communications'
)
BEGIN
    INSERT INTO acesso.permissoes (
        chave,
        chave_legado,
        nome,
        descricao,
        recurso,
        acao,
        rota
    ) VALUES (
        'dashboard.can_manage_communications.read',
        'can_manage_communications',
        N'Alterar comunicações',
        N'Criar, editar e arquivar comunicados exibidos na Home',
        'can_manage_communications',
        'read',
        '/'
    );
END;
GO

DECLARE @setorAdminId BIGINT = (
    SELECT TOP 1 id
    FROM acesso.setores
    WHERE chave = 'setor-admin'
);

IF @setorAdminId IS NOT NULL
BEGIN
    INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
    SELECT @setorAdminId, p.id
    FROM acesso.permissoes p
    WHERE p.chave_legado IN ('can_manage_kpi_goals', 'can_manage_communications')
      AND NOT EXISTS (
          SELECT 1
          FROM acesso.setor_permissao_templates t
          WHERE t.setor_id = @setorAdminId
            AND t.permissao_id = p.id
      );
END;
GO
