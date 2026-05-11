IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 51300, 'Database ETL_SISTEMA nao encontrada para alimentar horarios de corte via Raster.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.raster_viagens', N'U') IS NULL
BEGIN
    THROW 51301, 'Tabela ETL_SISTEMA.dbo.raster_viagens nao encontrada. Aplique a ingestao Raster da ETL antes desta migration.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.raster_viagem_paradas', N'U') IS NULL
BEGIN
    THROW 51302, 'Tabela ETL_SISTEMA.dbo.raster_viagem_paradas nao encontrada. Aplique a ingestao Raster da ETL antes desta migration.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'raster_viagens', N'cod_solicitacao'),
        (N'raster_viagens', N'status_viagem'),
        (N'raster_viagens', N'placa_veiculo'),
        (N'raster_viagens', N'data_hora_prev_fim'),
        (N'raster_viagens', N'data_hora_real_ini'),
        (N'raster_viagens', N'data_hora_real_fim'),
        (N'raster_viagens', N'data_hora_identificou_fim_viagem'),
        (N'raster_viagens', N'rota_descricao'),
        (N'raster_viagens', N'data_extracao'),
        (N'raster_viagem_paradas', N'cod_solicitacao'),
        (N'raster_viagem_paradas', N'data_extracao')
    ) AS obrigatorias(tabela, nome)
    WHERE NOT EXISTS (
        SELECT 1
        FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c
        WHERE c.TABLE_SCHEMA = N'dbo'
          AND c.TABLE_NAME = obrigatorias.tabela
          AND c.COLUMN_NAME = obrigatorias.nome
    )
)
BEGIN
    THROW 51303, 'Tabelas Raster da ETL sem colunas exigidas pelo indicador Horario de Corte.', 1;
END;

EXEC(N'
CREATE OR ALTER VIEW dbo.vw_horarios_corte_powerbi
AS
WITH paradas AS (
    SELECT
        p.cod_solicitacao,
        MAX(p.data_extracao) AS parada_data_extracao
    FROM [ETL_SISTEMA].dbo.raster_viagem_paradas p
    GROUP BY p.cod_solicitacao
),
viagens AS (
    SELECT
        v.cod_solicitacao,
        v.status_viagem,
        v.placa_veiculo,
        NULLIF(LTRIM(RTRIM(REPLACE(REPLACE(REPLACE(v.rota_descricao, N''/BRASIL'', N''''), CHAR(13), N'' ''), CHAR(10), N'' ''))), N'''') AS rota_limpa,
        CAST(v.data_hora_real_ini AS DATETIME2(0)) AS data_hora_real_ini_at,
        CAST(v.data_hora_real_fim AS DATETIME2(0)) AS data_hora_real_fim_at,
        CAST(v.data_hora_prev_fim AS DATETIME2(0)) AS data_hora_prev_fim_at,
        CAST(v.data_hora_identificou_fim_viagem AS DATETIME2(0)) AS data_hora_identificou_fim_at,
        CAST(COALESCE(v.data_hora_real_fim, v.data_hora_prev_fim, v.data_hora_identificou_fim_viagem) AS DATETIME2(0)) AS data_corte_base_at,
        CASE
            WHEN p.parada_data_extracao IS NULL THEN v.data_extracao
            WHEN v.data_extracao IS NULL THEN p.parada_data_extracao
            WHEN p.parada_data_extracao > v.data_extracao THEN p.parada_data_extracao
            ELSE v.data_extracao
        END AS data_extracao_at
    FROM [ETL_SISTEMA].dbo.raster_viagens v
    LEFT JOIN paradas p
        ON p.cod_solicitacao = v.cod_solicitacao
),
partes_rota AS (
    SELECT
        v.*,
        CHARINDEX(N'' ATE '', UPPER(v.rota_limpa)) AS separador_ate
    FROM viagens v
),
rota AS (
    SELECT
        p.*,
        NULLIF(LTRIM(RTRIM(CASE
            WHEN p.separador_ate > 0 THEN LEFT(p.rota_limpa, p.separador_ate - 1)
            ELSE p.rota_limpa
        END)), N'''') AS origem_raw,
        NULLIF(LTRIM(RTRIM(CASE
            WHEN p.separador_ate > 0 THEN SUBSTRING(p.rota_limpa, p.separador_ate + 5, 500)
            ELSE NULL
        END)), N'''') AS destino_raw
    FROM partes_rota p
),
rota_canonica AS (
    SELECT
        r.*,
        CASE
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%AGUDOS/SP%'' THEN N''AGUDOS/SP - RODOGARCIA FILIAL AGU''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%CAMPINAS/SP%'' THEN N''CAMPINAS/SP - RODOGARCIA FILIAL CPQ''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%CASTRO/PR%'' THEN N''CASTRO/PR - RODOGARCIA FILIAL CAS''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%SAO JOSE DOS PINHAIS/PR%'' THEN N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%OSASCO/SP%'' THEN N''OSASCO/SP - RODOGARCIA FILIAL SPO''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%DUQUE DE CAXIAS/RJ%'' THEN N''DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%JABOATAO DOS GUARARAPES/PE%'' THEN N''JABOATAO DOS GUARARAPES/PE - RODOGARCIA FILIAL REC''
            WHEN UPPER(r.origem_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.origem_raw) LIKE N''%NOVO HAMBURGO/RS%'' THEN N''NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB''
            ELSE NULL
        END AS origem_sm,
        CASE
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%AGUDOS/SP%'' THEN N''AGUDOS/SP - RODOGARCIA FILIAL AGU''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%CAMPINAS/SP%'' THEN N''CAMPINAS/SP - RODOGARCIA FILIAL CPQ''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%CASTRO/PR%'' THEN N''CASTRO/PR - RODOGARCIA FILIAL CAS''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%SAO JOSE DOS PINHAIS/PR%'' THEN N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%OSASCO/SP%'' THEN N''OSASCO/SP - RODOGARCIA FILIAL SPO''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%DUQUE DE CAXIAS/RJ%'' THEN N''DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%JABOATAO DOS GUARARAPES/PE%'' THEN N''JABOATAO DOS GUARARAPES/PE - RODOGARCIA FILIAL REC''
            WHEN UPPER(r.destino_raw) LIKE N''%RODOGARCIA%'' AND UPPER(r.destino_raw) LIKE N''%NOVO HAMBURGO/RS%'' THEN N''NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB''
            ELSE NULL
        END AS destino_sm
    FROM rota r
),
hc_apoio AS (
    SELECT *
    FROM (VALUES
        (N''AGUDOS/SP - RODOGARCIA FILIAL AGU'', N''OSASCO/SP - RODOGARCIA FILIAL SPO'', CAST(N''20:30:00'' AS TIME(0))),
        (N''CAMPINAS/SP - RODOGARCIA FILIAL CPQ'', N''OSASCO/SP - RODOGARCIA FILIAL SPO'', CAST(N''20:30:00'' AS TIME(0))),
        (N''CASTRO/PR - RODOGARCIA FILIAL CAS'', N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB'', CAST(N''20:00:00'' AS TIME(0))),
        (N''CASTRO/PR - RODOGARCIA FILIAL CAS'', N''OSASCO/SP - RODOGARCIA FILIAL SPO'', CAST(N''22:30:00'' AS TIME(0))),
        (N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB'', N''NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB'', CAST(N''18:00:00'' AS TIME(0))),
        (N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB'', N''CASTRO/PR - RODOGARCIA FILIAL CAS'', CAST(N''01:00:00'' AS TIME(0))),
        (N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB'', N''OSASCO/SP - RODOGARCIA FILIAL SPO'', CAST(N''00:30:00'' AS TIME(0))),
        (N''OSASCO/SP - RODOGARCIA FILIAL SPO'', N''CASTRO/PR - RODOGARCIA FILIAL CAS'', CAST(N''23:00:00'' AS TIME(0))),
        (N''OSASCO/SP - RODOGARCIA FILIAL SPO'', N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB'', CAST(N''23:30:00'' AS TIME(0))),
        (N''OSASCO/SP - RODOGARCIA FILIAL SPO'', N''DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR'', CAST(N''23:30:00'' AS TIME(0))),
        (N''OSASCO/SP - RODOGARCIA FILIAL SPO'', N''AGUDOS/SP - RODOGARCIA FILIAL AGU'', CAST(N''02:30:00'' AS TIME(0))),
        (N''OSASCO/SP - RODOGARCIA FILIAL SPO'', N''CAMPINAS/SP - RODOGARCIA FILIAL CPQ'', CAST(N''04:00:00'' AS TIME(0))),
        (N''OSASCO/SP - RODOGARCIA FILIAL SPO'', N''JABOATAO DOS GUARARAPES/PE - RODOGARCIA FILIAL REC'', CAST(N''04:00:00'' AS TIME(0))),
        (N''DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR'', N''OSASCO/SP - RODOGARCIA FILIAL SPO'', CAST(N''05:00:00'' AS TIME(0))),
        (N''JABOATAO DOS GUARARAPES/PE - RODOGARCIA FILIAL REC'', N''OSASCO/SP - RODOGARCIA FILIAL SPO'', CAST(N''22:00:00'' AS TIME(0))),
        (N''NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB'', N''SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB'', CAST(N''04:00:00'' AS TIME(0)))
    ) AS apoio(origem_sm, destino_sm, horario_corte)
),
calculado AS (
    SELECT
        rc.*,
        CONCAT(rc.origem_sm, N'' x '', rc.destino_sm) AS origem_destino,
        apoio.horario_corte,
        CAST(rc.data_corte_base_at AS DATE) AS data_corte,
        CASE
            WHEN rc.data_corte_base_at IS NULL OR apoio.horario_corte IS NULL THEN NULL
            ELSE DATEADD(SECOND, DATEDIFF(SECOND, CAST(N''00:00:00'' AS TIME), apoio.horario_corte), CAST(CAST(rc.data_corte_base_at AS DATE) AS DATETIME2(0)))
        END AS corte_at
    FROM rota_canonica rc
    LEFT JOIN hc_apoio apoio
        ON apoio.origem_sm = rc.origem_sm
       AND apoio.destino_sm = rc.destino_sm
)
SELECT
    CONVERT(BIGINT, cod_solicitacao) AS [ID],
    data_corte AS [Data],
    N''Não mapeada'' AS [Filial],
    COALESCE(origem_destino, rota_limpa, CONCAT(N''SM '', CONVERT(NVARCHAR(30), cod_solicitacao))) AS [Linha ou Operação],
    origem_sm AS [Origem SM],
    destino_sm AS [Destino SM],
    origem_destino AS [Origem Destino],
    CASE WHEN origem_sm IS NULL THEN origem_raw ELSE LEFT(origem_sm, CHARINDEX(N'' - '', origem_sm + N'' - '') - 1) END AS [Origem],
    N''1º'' AS [Ordem],
    CASE WHEN destino_sm IS NULL THEN destino_raw ELSE LEFT(destino_sm, CHARINDEX(N'' - '', destino_sm + N'' - '') - 1) END AS [Destino],
    CASE WHEN horario_corte IS NULL THEN NULL ELSE CONVERT(CHAR(5), horario_corte, 108) END AS [Horario Corte SM],
    CASE WHEN data_hora_prev_fim_at IS NULL THEN NULL ELSE CONVERT(CHAR(5), CAST(data_hora_prev_fim_at AS TIME), 108) END AS [Previsao Chegada Destino],
    CASE
        WHEN data_hora_real_ini_at IS NULL OR COALESCE(data_hora_real_fim_at, data_hora_prev_fim_at) IS NULL THEN NULL
        WHEN DATEDIFF(MINUTE, data_hora_real_ini_at, COALESCE(data_hora_real_fim_at, data_hora_prev_fim_at)) < 0 THEN NULL
        ELSE CONCAT(
            CASE
                WHEN DATEDIFF(MINUTE, data_hora_real_ini_at, COALESCE(data_hora_real_fim_at, data_hora_prev_fim_at)) / 60 < 100
                    THEN RIGHT(N''00'' + CONVERT(NVARCHAR(2), DATEDIFF(MINUTE, data_hora_real_ini_at, COALESCE(data_hora_real_fim_at, data_hora_prev_fim_at)) / 60), 2)
                ELSE CONVERT(NVARCHAR(10), DATEDIFF(MINUTE, data_hora_real_ini_at, COALESCE(data_hora_real_fim_at, data_hora_prev_fim_at)) / 60)
            END,
            N'':'',
            RIGHT(N''00'' + CONVERT(NVARCHAR(2), DATEDIFF(MINUTE, data_hora_real_ini_at, COALESCE(data_hora_real_fim_at, data_hora_prev_fim_at)) % 60), 2)
        )
    END AS [Transit Time],
    CAST(data_hora_real_ini_at AS TIME(0)) AS [Início],
    CAST(NULL AS TIME(0)) AS [Manifestado],
    CAST(data_hora_real_ini_at AS TIME(0)) AS [SM Gerada],
    CAST(corte_at AS TIME(0)) AS [Corte],
    data_hora_real_ini_at AS [Saída Efetiva],
    corte_at AS [Horário de Corte],
    CASE
        WHEN data_hora_real_ini_at IS NULL OR corte_at IS NULL THEN NULL
        WHEN data_hora_real_ini_at <= corte_at THEN CAST(1 AS BIT)
        ELSE CAST(0 AS BIT)
    END AS [Saiu no Horário],
    CASE
        WHEN data_hora_real_ini_at IS NULL OR corte_at IS NULL THEN NULL
        ELSE DATEDIFF(MINUTE, corte_at, data_hora_real_ini_at)
    END AS [Atraso Minutos],
    CONCAT(
        N''Raster API | SM '',
        CONVERT(NVARCHAR(30), cod_solicitacao),
        N'' | Placa '',
        COALESCE(placa_veiculo, N''''),
        N'' | Status '',
        COALESCE(status_viagem, N''''),
        CASE WHEN horario_corte IS NULL THEN N'' | Sem horario de corte em HC Apoio'' ELSE N'''' END
    ) AS [Observação],
    N''Raster API - SQL Server'' AS [Nome do Arquivo],
    data_extracao_at AS [Importado em],
    N''ETL_SISTEMA.raster_viagens'' AS [Importado por],
    data_extracao_at AS [Data de extracao]
FROM calculado
WHERE data_corte IS NOT NULL;
');

PRINT 'Migration V013 concluida: vw_horarios_corte_powerbi agora calcula Horario de Corte com Raster e HC Apoio.';
GO
