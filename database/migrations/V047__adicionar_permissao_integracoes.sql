IF NOT EXISTS (
    SELECT 1
    FROM acesso.permissoes
    WHERE chave_legado = 'integracoes'
)
BEGIN
    INSERT INTO acesso.permissoes (
        chave,
        chave_legado,
        nome,
        descricao,
        recurso,
        acao,
        rota
    ) VALUES (
        'dashboard.integracoes.read',
        'integracoes',
        N'Integrações',
        N'Auditoria operacional de XML e comprovantes por cliente',
        'integracoes',
        'read',
        '/painel/integracoes'
    );
END;

DECLARE @permissaoIntegracoesId BIGINT = (
    SELECT TOP 1 id
    FROM acesso.permissoes
    WHERE chave_legado = 'integracoes'
);

DECLARE @permissaoFaturamentoId BIGINT = (
    SELECT TOP 1 id
    FROM acesso.permissoes
    WHERE chave_legado = 'fretes'
);

IF @permissaoIntegracoesId IS NOT NULL
BEGIN
    INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
    SELECT s.id, @permissaoIntegracoesId
    FROM acesso.setores s
    WHERE (
            s.chave = 'setor-admin'
            OR EXISTS (
                SELECT 1
                FROM acesso.setor_permissao_templates t
                WHERE t.setor_id = s.id
                  AND t.permissao_id = @permissaoFaturamentoId
            )
        )
      AND NOT EXISTS (
          SELECT 1
          FROM acesso.setor_permissao_templates existente
          WHERE existente.setor_id = s.id
            AND existente.permissao_id = @permissaoIntegracoesId
      );
END;
