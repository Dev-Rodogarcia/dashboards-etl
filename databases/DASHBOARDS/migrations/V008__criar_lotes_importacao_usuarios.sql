CREATE TABLE acesso.usuario_importacao_lotes (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    token_importacao    VARCHAR(80)   NOT NULL UNIQUE,
    arquivo_nome        NVARCHAR(255) NOT NULL,
    payload_json        NVARCHAR(MAX) NOT NULL,
    criado_por          VARCHAR(80)   NULL,
    criado_em           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    expira_em           DATETIME2     NOT NULL
);

CREATE INDEX IX_usuario_importacao_lotes_expira_em
    ON acesso.usuario_importacao_lotes(expira_em);
