IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 51800, 'Database ETL_SISTEMA nao encontrada para atualizar o wrapper de fretes.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_fretes_powerbi', N'V') IS NULL
BEGIN
    THROW 51801, 'View ETL_SISTEMA.dbo.vw_fretes_powerbi nao encontrada. Aplique as views da ETL antes desta migration.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'ID'),
        (N'Nº Minuta'),
        (N'Data frete'),
        (N'CT-e Emissão'),
        (N'Valor Total do Serviço'),
        (N'Valor Frete'),
        (N'Região Destino'),
        (N'Cidade Destino'),
        (N'Comprovante Anexado'),
        (N'Cortesia Flag'),
        (N'Classificação'),
        (N'data_referencia_faturamento'),
        (N'is_elegivel_faturamento')
    ) AS obrigatorias(nome)
    WHERE NOT EXISTS (
        SELECT 1
        FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c
        WHERE c.TABLE_SCHEMA = N'dbo'
          AND c.TABLE_NAME = N'vw_fretes_powerbi'
          AND c.COLUMN_NAME = obrigatorias.nome
    )
)
BEGIN
    THROW 51802, 'View de fretes da ETL sem colunas exigidas pelo dashboard. Aplique a migration 016_materializar_faturamento_fretes do ETL antes do Dashboard.', 1;
END;

EXEC(N'
CREATE OR ALTER VIEW dbo.vw_fretes_powerbi
AS
SELECT *
FROM [ETL_SISTEMA].dbo.vw_fretes_powerbi;
');

EXEC sys.sp_refreshview N'dbo.vw_fretes_powerbi';
