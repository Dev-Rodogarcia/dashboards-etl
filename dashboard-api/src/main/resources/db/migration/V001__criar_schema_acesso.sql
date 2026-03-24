-- =============================================================
-- Schema de controle de acesso (autorizacao + auditoria)
-- Banco: ETL_SISTEMA  |  Schema: acesso
-- =============================================================

CREATE SCHEMA acesso;
GO

-- -------------------------------------------------------------
-- SETORES (departamentos / areas de negocio)
-- -------------------------------------------------------------
CREATE TABLE acesso.setores (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    chave           VARCHAR(80)   NOT NULL UNIQUE,
    nome            NVARCHAR(120) NOT NULL UNIQUE,
    descricao       NVARCHAR(500) NULL,
    sistema         BIT           NOT NULL DEFAULT 0,
    ativo           BIT           NOT NULL DEFAULT 1,
    criado_em       DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    atualizado_em   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);

-- -------------------------------------------------------------
-- PERMISSOES (catalogo de permissoes)
-- -------------------------------------------------------------
CREATE TABLE acesso.permissoes (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    chave           VARCHAR(100)  NOT NULL UNIQUE,
    chave_legado    VARCHAR(50)   NULL UNIQUE,
    nome            NVARCHAR(120) NOT NULL,
    descricao       NVARCHAR(500) NULL,
    recurso         VARCHAR(60)   NULL,
    acao            VARCHAR(30)   NOT NULL DEFAULT 'read',
    rota            VARCHAR(120)  NULL,
    ativo           BIT           NOT NULL DEFAULT 1,
    criado_em       DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);

-- -------------------------------------------------------------
-- PAPEIS (roles administrativos)
-- -------------------------------------------------------------
CREATE TABLE acesso.papeis (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    nome            VARCHAR(60)   NOT NULL UNIQUE,
    descricao       NVARCHAR(300) NULL,
    nivel           INT           NOT NULL DEFAULT 0,
    ativo           BIT           NOT NULL DEFAULT 1,
    criado_em       DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);

-- -------------------------------------------------------------
-- USUARIOS
-- -------------------------------------------------------------
CREATE TABLE acesso.usuarios (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    chave_legado        VARCHAR(80)  NULL UNIQUE,
    login               VARCHAR(80)  NOT NULL UNIQUE,
    nome                NVARCHAR(200) NOT NULL,
    email               VARCHAR(254) NOT NULL UNIQUE,
    senha_hash          VARCHAR(255) NOT NULL,
    algoritmo_hash      VARCHAR(20)  NOT NULL DEFAULT 'bcrypt',
    senha_alterada_em   DATETIME2    NULL,
    exige_troca_senha   BIT          NOT NULL DEFAULT 0,
    tentativas_falha    INT          NOT NULL DEFAULT 0,
    bloqueado_ate       DATETIME2    NULL,
    identity_source     VARCHAR(30)  NOT NULL DEFAULT 'local',
    external_subject_id VARCHAR(255) NULL,
    mfa_status          VARCHAR(20)  NOT NULL DEFAULT 'disabled',
    setor_id            BIGINT       NOT NULL REFERENCES acesso.setores(id),
    ativo               BIT          NOT NULL DEFAULT 1,
    criado_em           DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    atualizado_em       DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME()
);

-- -------------------------------------------------------------
-- SETOR_PERMISSAO_TEMPLATES (baseline de permissoes por setor)
-- -------------------------------------------------------------
CREATE TABLE acesso.setor_permissao_templates (
    setor_id        BIGINT NOT NULL REFERENCES acesso.setores(id),
    permissao_id    BIGINT NOT NULL REFERENCES acesso.permissoes(id),
    PRIMARY KEY (setor_id, permissao_id)
);

-- -------------------------------------------------------------
-- USUARIO_PAPEL_VINCULOS (atribuicao de papeis a usuarios)
-- -------------------------------------------------------------
CREATE TABLE acesso.usuario_papel_vinculos (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id      BIGINT   NOT NULL REFERENCES acesso.usuarios(id),
    papel_id        BIGINT   NOT NULL REFERENCES acesso.papeis(id),
    concedido_por   BIGINT   NULL     REFERENCES acesso.usuarios(id),
    concedido_em    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE(usuario_id, papel_id)
);

-- -------------------------------------------------------------
-- USUARIO_PERMISSAO_OVERRIDES (GRANT/DENY por usuario)
-- -------------------------------------------------------------
CREATE TABLE acesso.usuario_permissao_overrides (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id      BIGINT    NOT NULL REFERENCES acesso.usuarios(id),
    permissao_id    BIGINT    NOT NULL REFERENCES acesso.permissoes(id),
    tipo            VARCHAR(5) NOT NULL CHECK (tipo IN ('GRANT','DENY')),
    concedido_por   BIGINT    NULL     REFERENCES acesso.usuarios(id),
    concedido_em    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE(usuario_id, permissao_id)
);

-- -------------------------------------------------------------
-- AUDIT_LOGS
-- -------------------------------------------------------------
CREATE TABLE acesso.audit_logs (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    timestamp_utc   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    usuario_id      BIGINT        NULL     REFERENCES acesso.usuarios(id),
    usuario_login   VARCHAR(80)   NULL,
    acao            VARCHAR(60)   NOT NULL,
    recurso         VARCHAR(120)  NULL,
    detalhes_json   NVARCHAR(MAX) NULL,
    ip_address      VARCHAR(45)   NULL,
    user_agent      VARCHAR(500)  NULL
);

CREATE INDEX IX_audit_timestamp ON acesso.audit_logs(timestamp_utc DESC);
CREATE INDEX IX_audit_usuario   ON acesso.audit_logs(usuario_id, timestamp_utc DESC);
CREATE INDEX IX_audit_acao      ON acesso.audit_logs(acao, timestamp_utc DESC);
