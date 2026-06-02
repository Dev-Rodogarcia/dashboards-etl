EXEC(N'
CREATE OR ALTER VIEW dbo.vw_horarios_corte_powerbi
AS
WITH base AS (
    SELECT
        hc.id,
        hc.data_operacao,
        COALESCE(NULLIF(LTRIM(RTRIM(hc.filial_canonica)), ''''), N''Não mapeada'') AS filial_canonica,
        hc.linha_ou_operacao_original,
        hc.inicio,
        hc.manifestado,
        hc.sm_gerada,
        hc.corte,
        hc.observacao,
        hc.nome_arquivo,
        hc.importado_em,
        hc.importado_por,
        CASE
            WHEN hc.inicio IS NULL THEN NULL
            ELSE DATEADD(SECOND, DATEDIFF(SECOND, CAST(''00:00:00'' AS TIME(0)), hc.inicio), CAST(hc.data_operacao AS DATETIME2(0)))
        END AS inicio_at,
        CASE
            WHEN hc.manifestado IS NULL THEN NULL
            ELSE DATEADD(DAY, CASE WHEN hc.inicio IS NOT NULL AND hc.manifestado < hc.inicio THEN 1 ELSE 0 END,
                DATEADD(SECOND, DATEDIFF(SECOND, CAST(''00:00:00'' AS TIME(0)), hc.manifestado), CAST(hc.data_operacao AS DATETIME2(0))))
        END AS manifestado_at,
        CASE
            WHEN hc.sm_gerada IS NULL THEN NULL
            ELSE DATEADD(DAY, CASE WHEN hc.inicio IS NOT NULL AND hc.sm_gerada < hc.inicio THEN 1 ELSE 0 END,
                DATEADD(SECOND, DATEDIFF(SECOND, CAST(''00:00:00'' AS TIME(0)), hc.sm_gerada), CAST(hc.data_operacao AS DATETIME2(0))))
        END AS sm_gerada_at,
        CASE
            WHEN hc.corte IS NULL THEN NULL
            ELSE DATEADD(SECOND, DATEDIFF(SECOND, CAST(''00:00:00'' AS TIME(0)), hc.corte), CAST(hc.data_operacao AS DATETIME2(0)))
        END AS corte_at
    FROM dbo.horarios_corte hc
)
SELECT
    id AS [ID],
    data_operacao AS [Data],
    filial_canonica AS [Filial],
    linha_ou_operacao_original AS [Linha ou Operação],
    inicio AS [Início],
    manifestado AS [Manifestado],
    sm_gerada AS [SM Gerada],
    corte AS [Corte],
    sm_gerada_at AS [Saída Efetiva],
    corte_at AS [Horário de Corte],
    CASE
        WHEN sm_gerada_at IS NULL OR corte_at IS NULL THEN NULL
        WHEN sm_gerada_at <= corte_at THEN CAST(1 AS BIT)
        ELSE CAST(0 AS BIT)
    END AS [Saiu no Horário],
    CASE
        WHEN sm_gerada_at IS NULL OR corte_at IS NULL THEN NULL
        ELSE DATEDIFF(MINUTE, corte_at, sm_gerada_at)
    END AS [Atraso Minutos],
    observacao AS [Observação],
    nome_arquivo AS [Nome do Arquivo],
    importado_em AS [Importado em],
    importado_por AS [Importado por],
    importado_em AS [Data de extracao]
FROM base;
');
