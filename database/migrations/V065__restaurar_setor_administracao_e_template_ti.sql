-- "TI - Admin" era o setor de sistema setor-admin renomeado indevidamente.
-- Administração permanece como setor técnico do sistema; administração é definida pelo papel do usuário.

DECLARE @setorTiId BIGINT = (
    SELECT id
    FROM acesso.setores
    WHERE chave = 'setor-ti'
);

DECLARE @setorAdminId BIGINT = (
    SELECT id
    FROM acesso.setores
    WHERE chave = 'setor-admin'
);

IF @setorTiId IS NULL OR @setorAdminId IS NULL
BEGIN
    THROW 56501, 'Setores de sistema TI ou Administração não encontrados.', 1;
END;

-- V064 migrou os usuários. O acesso administrativo não pode virar baseline de todo o setor TI.
DELETE template
FROM acesso.setor_permissao_templates template
INNER JOIN acesso.permissoes permissao ON permissao.id = template.permissao_id
WHERE template.setor_id = @setorTiId
  AND permissao.chave_legado NOT IN ('etlSaude', 'dimensoes', 'indicadoresGestaoAVista');

INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT @setorTiId, permissao.id
FROM acesso.permissoes permissao
WHERE permissao.chave_legado IN ('etlSaude', 'dimensoes', 'indicadoresGestaoAVista')
  AND NOT EXISTS (
      SELECT 1
      FROM acesso.setor_permissao_templates existente
      WHERE existente.setor_id = @setorTiId
        AND existente.permissao_id = permissao.id
  );

-- Reabilita o setor técnico de Administração, sem usuários vinculados a ele.
IF NOT EXISTS (
    SELECT 1
    FROM acesso.setores
    WHERE nome = N'Administração'
      AND id <> @setorAdminId
)
BEGIN
    UPDATE acesso.setores
    SET nome = N'Administração',
        descricao = N'Área com acesso total ao sistema',
        sistema = 1,
        ativo = 1,
        atualizado_em = SYSUTCDATETIME()
    WHERE id = @setorAdminId;
END;
