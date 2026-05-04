IF NOT EXISTS (
    SELECT 1
    FROM acesso.permissoes
    WHERE chave_legado = 'indicadoresGestaoAVista'
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
        'dashboard.indicadores_gestao_a_vista.read',
        'indicadoresGestaoAVista',
        N'Indicadores de Gestão à Vista',
        N'Dashboard operacional de indicadores de gestão à vista',
        'indicadores_gestao_a_vista',
        'read',
        '/indicadores-gestao-a-vista'
    );
END;

DECLARE @permissaoId BIGINT = (
    SELECT TOP 1 id
    FROM acesso.permissoes
    WHERE chave_legado = 'indicadoresGestaoAVista'
);

IF @permissaoId IS NOT NULL
BEGIN
    INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
    SELECT s.id, @permissaoId
    FROM acesso.setores s
    WHERE s.chave IN ('setor-admin', 'setor-logistica', 'setor-ti', 'setor-diretoria')
      AND NOT EXISTS (
          SELECT 1
          FROM acesso.setor_permissao_templates t
          WHERE t.setor_id = s.id
            AND t.permissao_id = @permissaoId
      );
END;
