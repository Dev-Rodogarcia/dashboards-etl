SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.usuarios', N'U') IS NULL
BEGIN
    THROW 55500, 'Tabela acesso.usuarios ausente. Aplique as migrations anteriores antes da V055.', 1;
END;
GO

IF COL_LENGTH(N'acesso.usuarios', N'ultima_atividade') IS NULL
BEGIN
    ALTER TABLE acesso.usuarios
        ADD ultima_atividade DATETIMEOFFSET NULL;
END;
GO

IF COL_LENGTH(N'acesso.usuarios', N'ultima_rota_acessada') IS NULL
BEGIN
    ALTER TABLE acesso.usuarios
        ADD ultima_rota_acessada VARCHAR(100) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_usuarios_ultima_atividade'
      AND object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
BEGIN
    CREATE INDEX IX_usuarios_ultima_atividade
        ON acesso.usuarios (ultima_atividade)
        INCLUDE (ativo, ultima_rota_acessada)
        WHERE ultima_atividade IS NOT NULL;
END;
GO
