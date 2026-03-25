-- =============================================================
-- Papel unico por usuario, limpeza de grants e sessao persistente
-- =============================================================

-- Forcar um unico papel por usuario preservando o de maior nivel.
WITH papeis_ordenados AS (
    SELECT
        upv.id,
        ROW_NUMBER() OVER (
            PARTITION BY upv.usuario_id
            ORDER BY p.nivel DESC, upv.concedido_em DESC, upv.id DESC
        ) AS rn
    FROM acesso.usuario_papel_vinculos upv
    INNER JOIN acesso.papeis p ON p.id = upv.papel_id
)
DELETE FROM acesso.usuario_papel_vinculos
WHERE id IN (
    SELECT id
    FROM papeis_ordenados
    WHERE rn > 1
);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_usuario_papel_unico'
      AND object_id = OBJECT_ID('acesso.usuario_papel_vinculos')
)
BEGIN
    CREATE UNIQUE INDEX UX_usuario_papel_unico
        ON acesso.usuario_papel_vinculos (usuario_id);
END
GO

-- O novo modelo aceita apenas negacao explicita no usuario.
DELETE FROM acesso.usuario_permissao_overrides
WHERE tipo = 'GRANT';
GO

-- Sincronizar login legado com email para usar email como identificador unificado.
UPDATE acesso.usuarios
SET login = LOWER(LTRIM(RTRIM(email)))
WHERE login <> LOWER(LTRIM(RTRIM(email)));
GO

CREATE TABLE acesso.refresh_tokens (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id           BIGINT        NOT NULL REFERENCES acesso.usuarios(id),
    token_hash           VARCHAR(128)  NOT NULL UNIQUE,
    expira_em            DATETIME2     NOT NULL,
    revogado_em          DATETIME2     NULL,
    substituido_por_hash VARCHAR(128)  NULL,
    criado_em            DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    criado_ip            VARCHAR(45)   NULL,
    user_agent           VARCHAR(500)  NULL
);
GO

CREATE INDEX IX_refresh_tokens_usuario
    ON acesso.refresh_tokens (usuario_id, revogado_em, expira_em DESC);
GO
