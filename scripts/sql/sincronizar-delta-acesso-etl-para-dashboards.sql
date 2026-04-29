:setvar SourceDb ETL_SISTEMA
:setvar TargetDb DASHBOARDS

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE [$(TargetDb)];
GO

BEGIN TRANSACTION;

SET IDENTITY_INSERT acesso.setores ON;
INSERT INTO acesso.setores (id, chave, nome, descricao, sistema, ativo, criado_em, atualizado_em)
SELECT id, chave, nome, descricao, sistema, ativo, criado_em, atualizado_em
FROM [$(SourceDb)].acesso.setores src
WHERE NOT EXISTS (SELECT 1 FROM acesso.setores tgt WHERE tgt.id = src.id);
SET IDENTITY_INSERT acesso.setores OFF;

SET IDENTITY_INSERT acesso.permissoes ON;
INSERT INTO acesso.permissoes (id, chave, chave_legado, nome, descricao, recurso, acao, rota, ativo, criado_em)
SELECT id, chave, chave_legado, nome, descricao, recurso, acao, rota, ativo, criado_em
FROM [$(SourceDb)].acesso.permissoes src
WHERE NOT EXISTS (SELECT 1 FROM acesso.permissoes tgt WHERE tgt.id = src.id);
SET IDENTITY_INSERT acesso.permissoes OFF;

SET IDENTITY_INSERT acesso.papeis ON;
INSERT INTO acesso.papeis (id, nome, descricao, nivel, ativo, criado_em)
SELECT id, nome, descricao, nivel, ativo, criado_em
FROM [$(SourceDb)].acesso.papeis src
WHERE NOT EXISTS (SELECT 1 FROM acesso.papeis tgt WHERE tgt.id = src.id)
  AND NOT EXISTS (SELECT 1 FROM acesso.papeis tgt WHERE tgt.nome = src.nome);
SET IDENTITY_INSERT acesso.papeis OFF;

SET IDENTITY_INSERT acesso.usuarios ON;
INSERT INTO acesso.usuarios (
    id,
    chave_legado,
    login,
    nome,
    email,
    senha_hash,
    algoritmo_hash,
    senha_alterada_em,
    exige_troca_senha,
    tentativas_falha,
    bloqueado_ate,
    identity_source,
    external_subject_id,
    mfa_status,
    setor_id,
    ativo,
    criado_em,
    atualizado_em
)
SELECT
    id,
    chave_legado,
    login,
    nome,
    email,
    senha_hash,
    algoritmo_hash,
    senha_alterada_em,
    exige_troca_senha,
    tentativas_falha,
    bloqueado_ate,
    identity_source,
    external_subject_id,
    mfa_status,
    setor_id,
    ativo,
    criado_em,
    atualizado_em
FROM [$(SourceDb)].acesso.usuarios src
WHERE NOT EXISTS (SELECT 1 FROM acesso.usuarios tgt WHERE tgt.id = src.id)
  AND NOT EXISTS (SELECT 1 FROM acesso.usuarios tgt WHERE LOWER(tgt.email) = LOWER(src.email));
SET IDENTITY_INSERT acesso.usuarios OFF;

INSERT INTO acesso.setor_filiais_permitidas (setor_id, filial_nome)
SELECT setor_id, filial_nome
FROM [$(SourceDb)].acesso.setor_filiais_permitidas src
WHERE NOT EXISTS (
    SELECT 1
    FROM acesso.setor_filiais_permitidas tgt
    WHERE tgt.setor_id = src.setor_id
      AND tgt.filial_nome = src.filial_nome
);

INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT setor_id, permissao_id
FROM [$(SourceDb)].acesso.setor_permissao_templates src
WHERE NOT EXISTS (
    SELECT 1
    FROM acesso.setor_permissao_templates tgt
    WHERE tgt.setor_id = src.setor_id
      AND tgt.permissao_id = src.permissao_id
);

SET IDENTITY_INSERT acesso.usuario_papel_vinculos ON;
INSERT INTO acesso.usuario_papel_vinculos (id, usuario_id, papel_id, concedido_por, concedido_em)
SELECT id, usuario_id, papel_id, concedido_por, concedido_em
FROM [$(SourceDb)].acesso.usuario_papel_vinculos src
INNER JOIN [$(SourceDb)].acesso.usuarios src_usuario ON src_usuario.id = src.usuario_id
INNER JOIN [$(SourceDb)].acesso.papeis src_papel ON src_papel.id = src.papel_id
WHERE EXISTS (SELECT 1 FROM acesso.usuarios u WHERE u.id = src.usuario_id)
  AND EXISTS (SELECT 1 FROM acesso.papeis p WHERE p.id = src.papel_id)
  AND (src.concedido_por IS NULL OR EXISTS (SELECT 1 FROM acesso.usuarios u WHERE u.id = src.concedido_por))
  AND NOT (
      LOWER(src_usuario.email) = 'desenvolvedor@rodogarcia.com.br'
      AND src_papel.nome <> 'desenvolvedor'
  )
  AND NOT EXISTS (SELECT 1 FROM acesso.usuario_papel_vinculos tgt WHERE tgt.id = src.id)
  AND NOT EXISTS (
      SELECT 1
      FROM acesso.usuario_papel_vinculos tgt
      WHERE tgt.usuario_id = src.usuario_id
        AND tgt.papel_id = src.papel_id
  );
SET IDENTITY_INSERT acesso.usuario_papel_vinculos OFF;

SET IDENTITY_INSERT acesso.usuario_permissao_overrides ON;
INSERT INTO acesso.usuario_permissao_overrides (id, usuario_id, permissao_id, tipo, concedido_por, concedido_em)
SELECT id, usuario_id, permissao_id, tipo, concedido_por, concedido_em
FROM [$(SourceDb)].acesso.usuario_permissao_overrides src
WHERE EXISTS (SELECT 1 FROM acesso.usuarios u WHERE u.id = src.usuario_id)
  AND EXISTS (SELECT 1 FROM acesso.permissoes p WHERE p.id = src.permissao_id)
  AND (src.concedido_por IS NULL OR EXISTS (SELECT 1 FROM acesso.usuarios u WHERE u.id = src.concedido_por))
  AND NOT EXISTS (SELECT 1 FROM acesso.usuario_permissao_overrides tgt WHERE tgt.id = src.id)
  AND NOT EXISTS (
      SELECT 1
      FROM acesso.usuario_permissao_overrides tgt
      WHERE tgt.usuario_id = src.usuario_id
        AND tgt.permissao_id = src.permissao_id
  );
SET IDENTITY_INSERT acesso.usuario_permissao_overrides OFF;

SET IDENTITY_INSERT acesso.audit_logs ON;
INSERT INTO acesso.audit_logs (id, timestamp_utc, usuario_id, usuario_login, acao, recurso, detalhes_json, ip_address, user_agent)
SELECT id, timestamp_utc, usuario_id, usuario_login, acao, recurso, detalhes_json, ip_address, user_agent
FROM [$(SourceDb)].acesso.audit_logs src
WHERE (src.usuario_id IS NULL OR EXISTS (SELECT 1 FROM acesso.usuarios u WHERE u.id = src.usuario_id))
  AND NOT EXISTS (SELECT 1 FROM acesso.audit_logs tgt WHERE tgt.id = src.id);
SET IDENTITY_INSERT acesso.audit_logs OFF;

SET IDENTITY_INSERT acesso.refresh_tokens ON;
INSERT INTO acesso.refresh_tokens (
    id,
    usuario_id,
    token_hash,
    expira_em,
    revogado_em,
    substituido_por_hash,
    criado_em,
    criado_ip,
    user_agent
)
SELECT
    id,
    usuario_id,
    token_hash,
    expira_em,
    revogado_em,
    substituido_por_hash,
    criado_em,
    criado_ip,
    user_agent
FROM [$(SourceDb)].acesso.refresh_tokens src
WHERE EXISTS (SELECT 1 FROM acesso.usuarios u WHERE u.id = src.usuario_id)
  AND NOT EXISTS (SELECT 1 FROM acesso.refresh_tokens tgt WHERE tgt.id = src.id)
  AND NOT EXISTS (SELECT 1 FROM acesso.refresh_tokens tgt WHERE tgt.token_hash = src.token_hash);
SET IDENTITY_INSERT acesso.refresh_tokens OFF;

COMMIT TRANSACTION;

SELECT
    s.name AS schema_name,
    t.name AS table_name,
    SUM(p.rows) AS total_rows
FROM sys.tables t
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
INNER JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
WHERE s.name = N'acesso'
GROUP BY s.name, t.name
ORDER BY t.name;
GO
