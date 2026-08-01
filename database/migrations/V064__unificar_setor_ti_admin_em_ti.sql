-- Consolida o setor legado "TI - Admin" no setor canônico "TI".
-- Papéis e exceções individuais dos usuários permanecem inalterados.

DECLARE @setorTiId BIGINT = (
    SELECT id
    FROM acesso.setores
    WHERE nome = N'TI'
);

DECLARE @setorTiAdminId BIGINT = (
    SELECT id
    FROM acesso.setores
    WHERE nome = N'TI - Admin'
      AND ativo = 1
);

IF @setorTiId IS NULL
BEGIN
    THROW 56401, 'Setor canônico TI não encontrado para consolidar TI - Admin.', 1;
END;

IF @setorTiAdminId IS NOT NULL
BEGIN
    -- Mantém a união dos acessos herdados pelos dois setores.
    INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
    SELECT @setorTiId, origem.permissao_id
    FROM acesso.setor_permissao_templates origem
    WHERE origem.setor_id = @setorTiAdminId
      AND NOT EXISTS (
          SELECT 1
          FROM acesso.setor_permissao_templates destino
          WHERE destino.setor_id = @setorTiId
            AND destino.permissao_id = origem.permissao_id
      );

    -- Preserva todas as filiais liberadas anteriormente para TI - Admin.
    INSERT INTO acesso.setor_filiais_permitidas (setor_id, filial_nome)
    SELECT @setorTiId, origem.filial_nome
    FROM acesso.setor_filiais_permitidas origem
    WHERE origem.setor_id = @setorTiAdminId
      AND NOT EXISTS (
          SELECT 1
          FROM acesso.setor_filiais_permitidas destino
          WHERE destino.setor_id = @setorTiId
            AND destino.filial_nome = origem.filial_nome
      );

    UPDATE acesso.usuarios
    SET setor_id = @setorTiId,
        atualizado_em = SYSUTCDATETIME()
    WHERE setor_id = @setorTiAdminId;

    -- Exclusão lógica: o setor duplicado deixa de ser oferecido no cadastro.
    UPDATE acesso.setores
    SET ativo = 0,
        atualizado_em = SYSUTCDATETIME()
    WHERE id = @setorTiAdminId;
END;
