SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.home_solicitacoes_melhoria (
        id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        tipo                 VARCHAR(30)          NOT NULL,
        titulo               NVARCHAR(140)        NOT NULL,
        descricao            NVARCHAR(2000)       NOT NULL,
        resultado_esperado   NVARCHAR(1000)       NULL,
        status               VARCHAR(20)          NOT NULL CONSTRAINT DF_home_solicitacoes_melhoria_status DEFAULT 'ABERTA',
        solicitante_nome     NVARCHAR(200)        NOT NULL,
        solicitante_email    NVARCHAR(254)        NOT NULL,
        ativo                BIT                  NOT NULL CONSTRAINT DF_home_solicitacoes_melhoria_ativo DEFAULT 1,
        criado_em            DATETIME2(0)         NOT NULL CONSTRAINT DF_home_solicitacoes_melhoria_criado_em DEFAULT SYSUTCDATETIME(),
        concluido_em         DATETIME2(0)         NULL,
        atualizado_por       NVARCHAR(120)        NULL,
        atualizado_em        DATETIME2(0)         NULL
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_home_solicitacoes_melhoria_tipo'
      AND parent_object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U')
)
BEGIN
    ALTER TABLE acesso.home_solicitacoes_melhoria
    WITH CHECK ADD CONSTRAINT CK_home_solicitacoes_melhoria_tipo
    CHECK (tipo IN ('MELHORIA', 'AUTOMACAO', 'DASHBOARD', 'CORRECAO', 'OUTRO'));
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_home_solicitacoes_melhoria_status'
      AND parent_object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U')
)
BEGIN
    ALTER TABLE acesso.home_solicitacoes_melhoria
    WITH CHECK ADD CONSTRAINT CK_home_solicitacoes_melhoria_status
    CHECK (status IN ('ABERTA', 'CONCLUIDA'));
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_home_solicitacoes_melhoria_ativo_status_criado'
      AND object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U')
)
BEGIN
    CREATE INDEX IX_home_solicitacoes_melhoria_ativo_status_criado
    ON acesso.home_solicitacoes_melhoria (ativo, status, criado_em DESC, id DESC);
END;
GO
