IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 51900, 'Database ETL_SISTEMA nao encontrada para sincronizar a view de localizacao de cargas.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_localizacao_cargas_powerbi', N'V') IS NULL
BEGIN
    THROW 51901, 'View ETL_SISTEMA.dbo.vw_localizacao_cargas_powerbi nao encontrada. Aplique as views da ETL antes desta migration.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'N° Minuta'),
        (N'Data do frete'),
        (N'Peso Taxado'),
        (N'Peso Taxado Decimal'),
        (N'Valor NF'),
        (N'Valor NF Decimal'),
        (N'Valor Frete'),
        (N'Filial Emissora'),
        (N'Previsão Entrega/Previsão de entrega'),
        (N'Região Destino'),
        (N'Filial Destino'),
        (N'Responsável pela Região de Destino'),
        (N'Sigla Responsável Região Destino'),
        (N'Status Carga'),
        (N'Status Normalizado'),
        (N'Status Terminal'),
        (N'Cancelado Flag'),
        (N'Filial Atual'),
        (N'Região Origem'),
        (N'Filial Origem'),
        (N'Localização Atual'),
        (N'Hash Localização'),
        (N'Data de extracao')
    ) AS obrigatorias(nome)
    WHERE NOT EXISTS (
        SELECT 1
        FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c
        WHERE c.TABLE_SCHEMA = N'dbo'
          AND c.TABLE_NAME = N'vw_localizacao_cargas_powerbi'
          AND c.COLUMN_NAME = obrigatorias.nome
    )
)
BEGIN
    THROW 51902, 'View de localizacao de cargas da ETL sem colunas exigidas pelo dashboard.', 1;
END;

EXEC(N'
CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi
AS
SELECT
    [Hora (Solicitacao)],
    [N° Minuta],
    [Tipo],
    [Data do frete],
    [Volumes],
    [Peso Taxado],
    [Peso Taxado Decimal],
    [Valor NF],
    [Valor NF Decimal],
    [Valor Frete],
    [Tipo Serviço],
    [Filial Emissora],
    [Previsão Entrega/Previsão de entrega],
    [Região Destino],
    [Filial Destino],
    [Responsável pela Região de Destino],
    [Sigla Responsável Região Destino],
    [Classificação],
    [Status Carga],
    [Status Normalizado],
    [Status Terminal],
    [Cancelado Flag],
    COALESCE(
        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(4000), [Filial Atual]))), N''''),
        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(4000), [Localização Atual]))), N'''')
    ) AS [Filial Atual],
    [Região Origem],
    [Filial Origem],
    [Localização Atual],
    [Hash Localização],
    [Metadata],
    [Data de extracao]
FROM [ETL_SISTEMA].dbo.vw_localizacao_cargas_powerbi;
');

EXEC sys.sp_refreshview N'dbo.vw_localizacao_cargas_powerbi';
