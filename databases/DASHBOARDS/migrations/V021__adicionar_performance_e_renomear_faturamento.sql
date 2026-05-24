IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 52100, 'Database ETL_SISTEMA nao encontrada para sincronizar Performance/Faturamento.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_fretes_powerbi', N'V') IS NULL
BEGIN
    THROW 52101, 'View ETL_SISTEMA.dbo.vw_fretes_powerbi nao encontrada. Aplique as migrations/views do ETL antes desta migration.', 1;
END;

EXEC(N'
CREATE OR ALTER VIEW dbo.vw_fretes_powerbi
AS
SELECT *
FROM [ETL_SISTEMA].dbo.vw_fretes_powerbi;
');

EXEC sys.sp_refreshview N'dbo.vw_fretes_powerbi';

UPDATE acesso.permissoes
SET nome = N'Faturamento',
    descricao = N'Dashboard operacional de faturamento',
    rota = N'/faturamento'
WHERE chave_legado = N'fretes';

IF NOT EXISTS (SELECT 1 FROM acesso.permissoes WHERE chave_legado = N'performance')
BEGIN
    INSERT INTO acesso.permissoes (chave, chave_legado, nome, descricao, recurso, acao, rota)
    VALUES (
        N'dashboard.performance.read',
        N'performance',
        N'Performance',
        N'Dashboard operacional de performance de entrega',
        N'performance',
        N'read',
        N'/performance'
    );
END;

INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s
JOIN acesso.permissoes p ON p.chave_legado = N'performance'
WHERE s.chave IN (N'setor-admin', N'setor-logistica', N'setor-ti', N'setor-diretoria')
  AND NOT EXISTS (
      SELECT 1
      FROM acesso.setor_permissao_templates existente
      WHERE existente.setor_id = s.id
        AND existente.permissao_id = p.id
  );
