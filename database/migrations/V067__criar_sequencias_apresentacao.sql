CREATE TABLE acesso.apresentacao_sequencias (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    usuario_id     BIGINT NOT NULL REFERENCES acesso.usuarios(id),
    nome           NVARCHAR(80) NOT NULL,
    ativo          BIT NOT NULL CONSTRAINT DF_apresentacao_sequencias_ativo DEFAULT 1,
    criado_em      DATETIME2 NOT NULL CONSTRAINT DF_apresentacao_sequencias_criado DEFAULT SYSUTCDATETIME(),
    atualizado_em  DATETIME2 NOT NULL CONSTRAINT DF_apresentacao_sequencias_atualizado DEFAULT SYSUTCDATETIME()
);

CREATE TABLE acesso.apresentacao_sequencia_itens (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sequencia_id   BIGINT NOT NULL REFERENCES acesso.apresentacao_sequencias(id),
    pagina         VARCHAR(60) NOT NULL,
    ordem          INT NOT NULL,
    CONSTRAINT CK_apresentacao_sequencia_itens_ordem_positiva CHECK (ordem > 0),
    CONSTRAINT UX_apresentacao_sequencia_itens_ordem UNIQUE (sequencia_id, ordem),
    CONSTRAINT UX_apresentacao_sequencia_itens_pagina UNIQUE (sequencia_id, pagina)
);

CREATE INDEX IX_apresentacao_sequencias_usuario_ativo
    ON acesso.apresentacao_sequencias (usuario_id, ativo, atualizado_em DESC);

CREATE INDEX IX_apresentacao_sequencia_itens_sequencia_ordem
    ON acesso.apresentacao_sequencia_itens (sequencia_id, ordem);
