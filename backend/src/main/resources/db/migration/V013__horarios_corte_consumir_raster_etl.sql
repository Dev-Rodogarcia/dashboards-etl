IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 51300, 'Database ETL_SISTEMA nao encontrada para alimentar horarios de corte via Raster.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_raster_sm_transit_time', N'V') IS NULL
BEGIN
    THROW 51301, 'View ETL_SISTEMA.dbo.vw_raster_sm_transit_time nao encontrada. Aplique a view Raster da ETL antes desta migration.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'origem_sm'),
        (N'destino_sm'),
        (N'origem_destino'),
        (N'origem_nome'),
        (N'ordem_parada_label'),
        (N'destino_nome'),
        (N'horario_corte_texto'),
        (N'previsao_chegada_destino'),
        (N'transit_time_texto'),
        (N'data_hora_prev_ini_raster'),
        (N'data_hora_real_ini'),
        (N'Data de extracao')
    ) AS obrigatorias(nome)
    WHERE NOT EXISTS (
        SELECT 1
        FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c
        WHERE c.TABLE_SCHEMA = N'dbo'
          AND c.TABLE_NAME = N'vw_raster_sm_transit_time'
          AND c.COLUMN_NAME = obrigatorias.nome
    )
)
BEGIN
    THROW 51302, 'View ETL_SISTEMA.dbo.vw_raster_sm_transit_time sem colunas Raster exigidas pelo dashboard. Atualize a view 022 da ETL antes desta migration.', 1;
END;

EXEC(N'
CREATE OR ALTER VIEW dbo.vw_horarios_corte_powerbi
AS
WITH raster AS (
    SELECT
        r.cod_solicitacao,
        COALESCE(r.ordem_parada, 0) AS ordem_parada,
        r.status_viagem,
        COALESCE(
            NULLIF(LTRIM(RTRIM(r.origem_destino)), ''''),
            NULLIF(LTRIM(RTRIM(r.origem_sm)), ''''),
            CONCAT(N''SM '', CONVERT(NVARCHAR(30), r.cod_solicitacao))
        ) AS linha_ou_operacao,
        NULLIF(LTRIM(RTRIM(r.origem_sm)), '''') AS origem_sm,
        NULLIF(LTRIM(RTRIM(r.destino_sm)), '''') AS destino_sm,
        NULLIF(LTRIM(RTRIM(r.origem_destino)), '''') AS origem_destino,
        NULLIF(LTRIM(RTRIM(r.origem_nome)), '''') AS origem,
        NULLIF(LTRIM(RTRIM(r.ordem_parada_label)), '''') AS ordem,
        NULLIF(LTRIM(RTRIM(r.destino_nome)), '''') AS destino,
        NULLIF(LTRIM(RTRIM(r.horario_corte_texto)), '''') AS horario_corte_sm,
        NULLIF(LTRIM(RTRIM(r.previsao_chegada_destino)), '''') AS previsao_chegada_destino,
        NULLIF(LTRIM(RTRIM(r.transit_time_texto)), '''') AS transit_time,
        CAST(r.data_hora_prev_ini_raster AS DATETIME2(0)) AS corte_at,
        CAST(r.data_hora_real_ini AS DATETIME2(0)) AS saida_efetiva_at,
        CAST(r.[Data de extracao] AS DATETIME2(0)) AS data_extracao_at
    FROM [ETL_SISTEMA].dbo.vw_raster_sm_transit_time r
    WHERE r.data_hora_prev_ini_raster IS NOT NULL
)
SELECT
    CONVERT(BIGINT, cod_solicitacao) * CONVERT(BIGINT, 1000)
        + CONVERT(BIGINT, ordem_parada) AS [ID],
    CAST(corte_at AS DATE) AS [Data],
    N''Não mapeada'' AS [Filial],
    linha_ou_operacao AS [Linha ou Operação],
    origem_sm AS [Origem SM],
    destino_sm AS [Destino SM],
    origem_destino AS [Origem Destino],
    origem AS [Origem],
    ordem AS [Ordem],
    destino AS [Destino],
    horario_corte_sm AS [Horario Corte SM],
    previsao_chegada_destino AS [Previsao Chegada Destino],
    transit_time AS [Transit Time],
    CAST(corte_at AS TIME(0)) AS [Início],
    CAST(NULL AS TIME(0)) AS [Manifestado],
    CAST(saida_efetiva_at AS TIME(0)) AS [SM Gerada],
    CAST(corte_at AS TIME(0)) AS [Corte],
    saida_efetiva_at AS [Saída Efetiva],
    corte_at AS [Horário de Corte],
    CASE
        WHEN saida_efetiva_at IS NULL THEN NULL
        WHEN saida_efetiva_at <= corte_at THEN CAST(1 AS BIT)
        ELSE CAST(0 AS BIT)
    END AS [Saiu no Horário],
    CASE
        WHEN saida_efetiva_at IS NULL THEN NULL
        ELSE DATEDIFF(MINUTE, corte_at, saida_efetiva_at)
    END AS [Atraso Minutos],
    CONCAT(
        N''Raster API | SM '',
        CONVERT(NVARCHAR(30), cod_solicitacao),
        N'' | Ordem '',
        CONVERT(NVARCHAR(12), ordem_parada),
        N'' | Status '',
        COALESCE(status_viagem, N'''')
    ) AS [Observação],
    N''Raster API - getEventoFimViagem'' AS [Nome do Arquivo],
    data_extracao_at AS [Importado em],
    N''ETL_SISTEMA'' AS [Importado por],
    data_extracao_at AS [Data de extracao]
FROM raster;
');

PRINT 'Migration V013 concluida: vw_horarios_corte_powerbi agora consome Raster da ETL.';
GO
