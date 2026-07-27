SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.home_solicitacoes_melhoria_anexos', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.home_solicitacoes_melhoria_anexos (
        id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        solicitacao_id  BIGINT                NOT NULL,
        nome_original   NVARCHAR(255)         NOT NULL,
        tipo_conteudo   VARCHAR(100)          NOT NULL,
        tamanho_bytes   BIGINT                NOT NULL,
        conteudo        VARBINARY(MAX)        NULL,
        ativo           BIT                   NOT NULL CONSTRAINT DF_home_solicitacoes_melhoria_anexos_ativo DEFAULT 1,
        criado_em       DATETIME2(0)          NOT NULL CONSTRAINT DF_home_solicitacoes_melhoria_anexos_criado_em DEFAULT SYSUTCDATETIME(),
        removido_em     DATETIME2(0)          NULL,
        CONSTRAINT FK_home_solicitacoes_melhoria_anexos_solicitacao
            FOREIGN KEY (solicitacao_id) REFERENCES acesso.home_solicitacoes_melhoria(id)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_home_solicitacoes_melhoria_anexos_solicitacao_ativo'
      AND object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria_anexos', N'U')
)
BEGIN
    CREATE INDEX IX_home_solicitacoes_melhoria_anexos_solicitacao_ativo
    ON acesso.home_solicitacoes_melhoria_anexos (solicitacao_id, ativo, id ASC);
END;
GO
