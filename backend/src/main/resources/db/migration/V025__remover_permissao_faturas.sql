DECLARE @permissoes_faturas TABLE (
    id BIGINT PRIMARY KEY
);

INSERT INTO @permissoes_faturas (id)
SELECT id
FROM acesso.permissoes
WHERE chave = 'dashboard.faturas.read'
   OR chave_legado = 'faturas'
   OR (recurso = 'faturas' AND rota = '/faturas');

DELETE FROM acesso.usuario_permissao_overrides
WHERE permissao_id IN (SELECT id FROM @permissoes_faturas);

DELETE FROM acesso.setor_permissao_templates
WHERE permissao_id IN (SELECT id FROM @permissoes_faturas);

DELETE FROM acesso.permissoes
WHERE id IN (SELECT id FROM @permissoes_faturas);
