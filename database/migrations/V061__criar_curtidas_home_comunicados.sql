SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.home_comunicado_curtidas', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.home_comunicado_curtidas (
        id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        comunicado_id   BIGINT NOT NULL REFERENCES acesso.home_comunicados(id),
        usuario_id      BIGINT NOT NULL REFERENCES acesso.usuarios(id),
        ativo           BIT NOT NULL CONSTRAINT DF_home_comunicado_curtidas_ativo DEFAULT 1,
        criado_em       DATETIME2(0) NOT NULL CONSTRAINT DF_home_comunicado_curtidas_criado_em DEFAULT SYSUTCDATETIME(),
        atualizado_em   DATETIME2(0) NOT NULL CONSTRAINT DF_home_comunicado_curtidas_atualizado_em DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UX_home_comunicado_curtidas_ativa'
      AND object_id = OBJECT_ID(N'acesso.home_comunicado_curtidas', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_home_comunicado_curtidas_ativa
        ON acesso.home_comunicado_curtidas (comunicado_id, usuario_id)
        WHERE ativo = 1;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'IX_home_comunicado_curtidas_comunicado_ativo'
      AND object_id = OBJECT_ID(N'acesso.home_comunicado_curtidas', N'U')
)
BEGIN
    CREATE INDEX IX_home_comunicado_curtidas_comunicado_ativo
        ON acesso.home_comunicado_curtidas (comunicado_id, ativo, usuario_id);
END;
GO
