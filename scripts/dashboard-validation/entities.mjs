function metric({
  id,
  label,
  uiLabel,
  sqlKey,
  apiKey,
  type = 'number',
  toleranceAbs,
  tolerancePct,
  critical = false,
}) {
  return {
    id,
    label,
    uiLabel,
    sqlKey,
    apiKey,
    type,
    toleranceAbs,
    tolerancePct,
    critical,
  };
}

function buildPeriodDeclarations({ dataInicio, dataFim }) {
  return `
DECLARE @DataInicio DATE = '${dataInicio}';
DECLARE @DataFim DATE = '${dataFim}';
DECLARE @Limite INT = 200;
DECLARE @Hoje DATE = CAST(GETDATE() AS DATE);
DECLARE @DataInicioOffset DATETIMEOFFSET = CAST(@DataInicio AS DATETIME2) AT TIME ZONE 'E. South America Standard Time';
DECLARE @DataFimExclusivoOffset DATETIMEOFFSET = CAST(DATEADD(DAY, 1, @DataFim) AS DATETIME2) AT TIME ZONE 'E. South America Standard Time';
`.trim();
}

function buildSqlJsonQuery({ period, cte = '', select }) {
  const cteSection = cte.trim() ? `\n${cte.trim()}\n` : '\n';
  return `
SET NOCOUNT ON;
${buildPeriodDeclarations(period)}
${cteSection}${select.trim()}
FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;
`.trim();
}

export const ENTITIES = [
  {
    key: 'coletas',
    label: 'Coletas',
    apiPath: '/api/painel/coletas',
    uiPath: '/coletas',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'total_coletas', label: 'Total Coletas', uiLabel: 'Total Coletas', sqlKey: 'total_coletas', apiKey: 'totalColetas', type: 'count', critical: true }),
      metric({ id: 'finalizadas', label: 'Finalizadas', uiLabel: 'Finalizadas', sqlKey: 'finalizadas', apiKey: 'finalizadas', type: 'count' }),
      metric({ id: 'taxa_sucesso_pct', label: 'Taxa Sucesso', uiLabel: 'Taxa Sucesso', sqlKey: 'taxa_sucesso_pct', apiKey: 'taxaSucesso', type: 'percentage', toleranceAbs: 0.5, critical: true }),
      metric({ id: 'taxa_cancelamento_pct', label: 'Cancelamento %', uiLabel: 'Cancelamento %', sqlKey: 'taxa_cancelamento_pct', apiKey: 'taxaCancelamento', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'sla_no_agendamento_pct', label: 'SLA Agendamento', uiLabel: 'SLA Agendamento', sqlKey: 'sla_no_agendamento_pct', apiKey: 'slaNoAgendamento', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'lead_time_medio_dias', label: 'Lead Time Médio', uiLabel: 'Lead Time Médio', sqlKey: 'lead_time_medio_dias', apiKey: 'leadTimeMedioDias', type: 'days', toleranceAbs: 0.01 }),
      metric({ id: 'tentativas_medias_dashboard', label: 'Tentativas Méd.', uiLabel: 'Tentativas Méd.', sqlKey: 'tentativas_medias_dashboard', apiKey: 'tentativasMedias', type: 'number', toleranceAbs: 0.01 }),
      metric({ id: 'peso_taxado_total', label: 'Peso Taxado', uiLabel: 'Peso Taxado', sqlKey: 'peso_taxado_total', apiKey: 'pesoTaxadoTotal', type: 'weight', toleranceAbs: 0.01 }),
      metric({ id: 'valor_nf_total', label: 'Valor NF', uiLabel: 'Valor NF', sqlKey: 'valor_nf_total', apiKey: 'valorNfTotal', type: 'currency', toleranceAbs: 0.01, critical: true }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base_raw AS (
    SELECT *
    FROM dbo.vw_coletas_powerbi
    WHERE [Solicitacao] BETWEEN @DataInicio AND @DataFim
),
base AS (
    SELECT *
    FROM (
        SELECT
            *,
            ROW_NUMBER() OVER (
                PARTITION BY [ID]
                ORDER BY [Solicitacao] DESC, [Numero Manifesto] DESC
            ) AS rn
        FROM base_raw
    ) t
    WHERE rn = 1
)`,
      select: `
SELECT
    COUNT(*) AS total_coletas,
    SUM(CASE WHEN [Status] IN (N'Finalizada', N'Coletada') THEN 1 ELSE 0 END) AS finalizadas,
    CAST(
        100.0 * SUM(CASE WHEN [Status] IN (N'Finalizada', N'Coletada') THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_sucesso_pct,
    CAST(
        100.0 * SUM(CASE WHEN [Status] = N'Cancelada' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_cancelamento_pct,
    CAST(
        100.0 * SUM(
            CASE
                WHEN [Status] IN (N'Finalizada', N'Coletada')
                 AND [Finalizacao] IS NOT NULL
                 AND [Agendamento] IS NOT NULL
                 AND [Finalizacao] <= [Agendamento]
                THEN 1 ELSE 0
            END
        )
        / NULLIF(SUM(CASE WHEN [Status] IN (N'Finalizada', N'Coletada') THEN 1 ELSE 0 END), 0)
        AS DECIMAL(10, 2)
    ) AS sla_no_agendamento_pct,
    CAST(AVG(
        CASE
            WHEN [Solicitacao] IS NOT NULL AND [Finalizacao] IS NOT NULL
            THEN DATEDIFF(DAY, [Solicitacao], [Finalizacao]) * 1.0
        END
    ) AS DECIMAL(10, 2)) AS lead_time_medio_dias,
    CAST(0.00 AS DECIMAL(10, 2)) AS tentativas_medias_dashboard,
    SUM(ISNULL([Peso Taxado], 0)) AS peso_taxado_total,
    SUM(ISNULL([Valor NF], 0)) AS valor_nf_total,
    MAX([Data de extracao]) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'fretes',
    label: 'Fretes',
    apiPath: '/api/painel/fretes',
    uiPath: '/fretes',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'total_fretes', label: 'Total de Fretes', uiLabel: 'Total de Fretes', sqlKey: 'total_fretes', apiKey: 'totalFretes', type: 'count', critical: true }),
      metric({ id: 'receita_bruta', label: 'Receita Bruta', uiLabel: 'Receita Bruta', sqlKey: 'receita_bruta', apiKey: 'receitaBruta', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'valor_frete', label: 'Valor Frete', uiLabel: 'Valor Frete', sqlKey: 'valor_frete', apiKey: 'valorFrete', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'ticket_medio', label: 'Ticket Médio', uiLabel: 'Ticket Médio', sqlKey: 'ticket_medio', apiKey: 'ticketMedio', type: 'currency', toleranceAbs: 0.01 }),
      metric({ id: 'peso_taxado_total', label: 'Peso Taxado', uiLabel: 'Peso Taxado', sqlKey: 'peso_taxado_total', apiKey: 'pesoTaxadoTotal', type: 'weight', toleranceAbs: 0.01 }),
      metric({ id: 'volumes_totais', label: 'Volumes', uiLabel: 'Volumes', sqlKey: 'volumes_totais', apiKey: 'volumesTotais', type: 'count' }),
      metric({ id: 'pct_cte_emitido', label: 'CT-e Emitido', uiLabel: 'CT-e Emitido', sqlKey: 'pct_cte_emitido', apiKey: 'pctCteEmitido', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'fretes_previsao_vencida', label: 'Previsão Vencida', uiLabel: 'Previsão Vencida', sqlKey: 'fretes_previsao_vencida', apiKey: 'fretesPrevisaoVencida', type: 'count', critical: true }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_fretes_powerbi
    WHERE [Data frete] >= @DataInicioOffset
      AND [Data frete] < @DataFimExclusivoOffset
)`,
      select: `
SELECT
    COUNT(*) AS total_fretes,
    SUM(ISNULL([Valor Total do Serviço], 0)) AS receita_bruta,
    SUM(ISNULL([Valor Frete], 0)) AS valor_frete,
    CAST(
        SUM(ISNULL([Valor Total do Serviço], 0)) / NULLIF(COUNT(*), 0)
        AS DECIMAL(18, 2)
    ) AS ticket_medio,
    SUM(ISNULL([Kg Taxado], 0)) AS peso_taxado_total,
    SUM(ISNULL([Volumes], 0)) AS volumes_totais,
    CAST(
        100.0 * SUM(CASE WHEN [CT-e ID] IS NOT NULL THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS pct_cte_emitido,
    CAST(
        100.0 * SUM(CASE WHEN [Nº NFS-e] IS NOT NULL THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS pct_nfse_emitida,
    SUM(
        CASE
            WHEN CAST([Previsão de Entrega] AS DATE) < @Hoje
             AND LOWER(ISNULL([Status], '')) <> 'finalizado'
            THEN 1 ELSE 0
        END
    ) AS fretes_previsao_vencida,
    MAX([Data de extracao]) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'faturas',
    label: 'Faturas',
    apiPath: '/api/painel/faturas',
    uiPath: '/faturas',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'valor_faturado', label: 'Valor Faturado', uiLabel: 'Valor Faturado', sqlKey: 'valor_faturado', apiKey: 'valorFaturado', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'valor_recebido', label: 'Valor Recebido', uiLabel: 'Valor Recebido', sqlKey: 'valor_recebido', apiKey: 'valorRecebido', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'saldo_aberto', label: 'Saldo Aberto', uiLabel: 'Saldo Aberto', sqlKey: 'saldo_aberto', apiKey: 'saldoAberto', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'taxa_adimplencia_pct', label: 'Adimplência %', uiLabel: 'Adimplência %', sqlKey: 'taxa_adimplencia_pct', apiKey: 'taxaAdimplencia', type: 'percentage', toleranceAbs: 0.5, critical: true }),
      metric({ id: 'dso_medio_dias', label: 'DSO Médio', uiLabel: 'DSO Médio', sqlKey: 'dso_medio_dias', apiKey: 'dsoMedioDias', type: 'days', toleranceAbs: 0.1 }),
      metric({ id: 'titulos_em_atraso', label: 'Tít. Atraso', uiLabel: 'Tít. Atraso', sqlKey: 'titulos_em_atraso', apiKey: 'titulosEmAtraso', type: 'count', critical: true }),
      metric({ id: 'clientes_ativos', label: 'Clientes Ativos', uiLabel: 'Clientes Ativos', sqlKey: 'clientes_ativos', apiKey: 'clientesAtivos', type: 'count' }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH titulos AS (
    SELECT *
    FROM dbo.vw_faturas_graphql_powerbi
    WHERE [Emissão] BETWEEN @DataInicio AND @DataFim
),
operacional AS (
    SELECT *
    FROM dbo.vw_faturas_por_cliente_powerbi
    WHERE [CT-e/Data de emissão] >= @DataInicioOffset
      AND [CT-e/Data de emissão] < @DataFimExclusivoOffset
)`,
      select: `
SELECT
    (SELECT SUM(ISNULL([Valor], 0)) FROM titulos) AS valor_faturado,
    (SELECT SUM(ISNULL([Valor Pago], 0)) FROM titulos) AS valor_recebido,
    (SELECT SUM(ISNULL([Valor a Pagar], 0)) FROM titulos) AS saldo_aberto,
    CAST(
        100.0 * (SELECT COUNT(*) FROM titulos WHERE [Pago] = N'Pago')
        / NULLIF((SELECT COUNT(*) FROM titulos), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_adimplencia_pct,
    CAST(
        (SELECT AVG(DATEDIFF(DAY, [Emissão], [Vencimento]) * 1.0)
         FROM titulos
         WHERE [Emissão] IS NOT NULL AND [Vencimento] IS NOT NULL)
        AS DECIMAL(10, 1)
    ) AS dso_medio_dias,
    (SELECT COUNT(*)
     FROM titulos
     WHERE [Vencimento] < @Hoje
       AND [Pago] <> N'Pago') AS titulos_em_atraso,
    (SELECT COUNT(DISTINCT [Pagador do frete/Nome])
     FROM operacional
     WHERE [Pagador do frete/Nome] IS NOT NULL
       AND LTRIM(RTRIM([Pagador do frete/Nome])) <> '') AS clientes_ativos,
    (
        SELECT MAX(dt)
        FROM (
            SELECT MAX(CAST([Data de extracao] AS DATETIME2)) AS dt FROM titulos
            UNION ALL
            SELECT MAX(CAST([Data da Última Atualização] AS DATETIME2)) AS dt FROM operacional
        ) u
    ) AS ultima_extracao`,
    }),
  },
  {
    key: 'cotacoes',
    label: 'Cotações',
    apiPath: '/api/painel/cotacoes',
    uiPath: '/cotacoes',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'total_cotacoes', label: 'Total Cotações', uiLabel: 'Total Cotações', sqlKey: 'total_cotacoes', apiKey: 'totalCotacoes', type: 'count', critical: true }),
      metric({ id: 'valor_potencial', label: 'Potencial (R$)', uiLabel: 'Potencial (R$)', sqlKey: 'valor_potencial', apiKey: 'valorPotencial', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'frete_medio', label: 'Frete Médio', uiLabel: 'Frete Médio', sqlKey: 'frete_medio', apiKey: 'freteMedio', type: 'currency', toleranceAbs: 0.01 }),
      metric({ id: 'frete_kg_medio', label: 'Frete/KG', uiLabel: 'Frete/KG', sqlKey: 'frete_kg_medio', apiKey: 'freteKgMedio', type: 'currency', toleranceAbs: 0.01 }),
      metric({ id: 'taxa_conversao_cte_pct', label: 'Conv. CT-e %', uiLabel: 'Conv. CT-e %', sqlKey: 'taxa_conversao_cte_pct', apiKey: 'taxaConversaoCte', type: 'percentage', toleranceAbs: 0.5, critical: true }),
      metric({ id: 'taxa_conversao_nfse_pct', label: 'Conv. NFS-e %', uiLabel: 'Conv. NFS-e %', sqlKey: 'taxa_conversao_nfse_pct', apiKey: 'taxaConversaoNfse', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'taxa_reprovacao_pct', label: 'Reprovação %', uiLabel: 'Reprovação %', sqlKey: 'taxa_reprovacao_pct', apiKey: 'taxaReprovacao', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'tempo_medio_conversao_horas', label: 'Conv. Médio (h)', uiLabel: 'Conv. Médio (h)', sqlKey: 'tempo_medio_conversao_horas', apiKey: 'tempoMedioConversaoHoras', type: 'hours', toleranceAbs: 0.01 }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_cotacoes_powerbi
    WHERE [Data Cotação] >= @DataInicioOffset
      AND [Data Cotação] < @DataFimExclusivoOffset
)`,
      select: `
SELECT
    COUNT(*) AS total_cotacoes,
    SUM(ISNULL([Valor frete], 0)) AS valor_potencial,
    CAST(
        SUM(ISNULL([Valor frete], 0)) / NULLIF(COUNT(*), 0)
        AS DECIMAL(18, 2)
    ) AS frete_medio,
    CAST(
        SUM(CASE WHEN ISNULL([Peso taxado], 0) > 0 THEN ISNULL([Valor frete], 0) ELSE 0 END)
        / NULLIF(SUM(CASE WHEN ISNULL([Peso taxado], 0) > 0 THEN [Peso taxado] ELSE 0 END), 0)
        AS DECIMAL(18, 2)
    ) AS frete_kg_medio,
    CAST(
        100.0 * SUM(CASE WHEN [Status Conversão] = 'Convertida' AND [CT-e/Data de emissão] IS NOT NULL THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_conversao_cte_pct,
    CAST(
        100.0 * SUM(CASE WHEN [Nfse/Data de emissão] IS NOT NULL THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_conversao_nfse_pct,
    CAST(
        100.0 * SUM(CASE WHEN [Status Conversão] = 'Reprovada' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_reprovacao_pct,
    CAST(AVG(
        CASE
            WHEN [CT-e/Data de emissão] IS NOT NULL AND [Data Cotação] IS NOT NULL
            THEN DATEDIFF(HOUR, [Data Cotação], [CT-e/Data de emissão]) * 1.0
        END
    ) AS DECIMAL(10, 2)) AS tempo_medio_conversao_horas,
    MAX([Data de extracao]) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'contas_a_pagar',
    label: 'Contas a Pagar',
    apiPath: '/api/painel/contas-a-pagar',
    uiPath: '/contas-a-pagar',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'valor_a_pagar', label: 'Valor a Pagar', uiLabel: 'Valor a Pagar', sqlKey: 'valor_a_pagar', apiKey: 'valorAPagar', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'valor_pago', label: 'Valor Pago', uiLabel: 'Valor Pago', sqlKey: 'valor_pago', apiKey: 'valorPago', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'saldo_aberto', label: 'Saldo Aberto', uiLabel: 'Saldo Aberto', sqlKey: 'saldo_aberto', apiKey: 'saldoAberto', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'taxa_liquidacao_pct', label: 'Taxa Liquidação', uiLabel: 'Taxa Liquidação', sqlKey: 'taxa_liquidacao_pct', apiKey: 'taxaLiquidacao', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'lead_time_liquidacao_dias', label: 'Lead Time', uiLabel: 'Lead Time', sqlKey: 'lead_time_liquidacao_dias', apiKey: 'leadTimeLiquidacaoDias', type: 'days', toleranceAbs: 0.1 }),
      metric({ id: 'pct_conciliado', label: '% Conciliado', uiLabel: '% Conciliado', sqlKey: 'pct_conciliado', apiKey: 'pctConciliado', type: 'percentage', toleranceAbs: 0.5 }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_contas_a_pagar_powerbi
    WHERE [Emissão] BETWEEN @DataInicio AND @DataFim
)`,
      select: `
SELECT
    SUM(ISNULL([Valor a pagar], 0)) AS valor_a_pagar,
    SUM(ISNULL([Valor pago], 0)) AS valor_pago,
    SUM(ISNULL([Valor a pagar], 0)) - SUM(ISNULL([Valor pago], 0)) AS saldo_aberto,
    CAST(
        100.0 * SUM(CASE WHEN [Pago] IN ('Sim', 'PAGO') THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_liquidacao_pct,
    CAST(AVG(
        CASE
            WHEN [Emissão] IS NOT NULL AND [Baixa/Data liquidação] IS NOT NULL
            THEN DATEDIFF(DAY, [Emissão], [Baixa/Data liquidação]) * 1.0
        END
    ) AS DECIMAL(10, 1)) AS lead_time_liquidacao_dias,
    CAST(
        100.0 * SUM(CASE WHEN [Conciliado] LIKE '%conciliado%' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS pct_conciliado,
    MAX([Data de extracao]) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'tracking',
    label: 'Localização de Cargas',
    apiPath: '/api/painel/tracking',
    uiPath: '/tracking',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'total_cargas', label: 'Total de Cargas', uiLabel: 'Total de Cargas', sqlKey: 'total_cargas', apiKey: 'totalCargas', type: 'count', critical: true }),
      metric({ id: 'em_transito', label: 'Em Trânsito', uiLabel: 'Em Trânsito', sqlKey: 'em_transito', apiKey: 'emTransito', type: 'count' }),
      metric({ id: 'previsao_vencida', label: 'Previsão Vencida', uiLabel: 'Previsão Vencida', sqlKey: 'previsao_vencida', apiKey: 'previsaoVencida', type: 'count', critical: true }),
      metric({ id: 'valor_frete_em_carteira', label: 'Val. Carteira', uiLabel: 'Val. Carteira', sqlKey: 'valor_frete_em_carteira', apiKey: 'valorFreteEmCarteira', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'peso_taxado_total', label: 'Peso Taxado', uiLabel: 'Peso Taxado', sqlKey: 'peso_taxado_total', apiKey: 'pesoTaxadoTotal', type: 'weight', toleranceAbs: 0.01 }),
      metric({ id: 'pct_finalizado', label: '% Finalizado', uiLabel: '% Finalizado', sqlKey: 'pct_finalizado', apiKey: 'pctFinalizado', type: 'percentage', toleranceAbs: 0.5 }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_localizacao_cargas_powerbi
    WHERE [Data do frete] >= @DataInicioOffset
      AND [Data do frete] < @DataFimExclusivoOffset
)`,
      select: `
SELECT
    COUNT(*) AS total_cargas,
    SUM(CASE WHEN [Status Carga] IN ('Em entrega', 'Em transferência', 'Manifestado') THEN 1 ELSE 0 END) AS em_transito,
    SUM(
        CASE
            WHEN CAST([Previsão Entrega/Previsão de entrega] AS DATE) < @Hoje
             AND [Status Carga] <> 'Finalizado'
            THEN 1 ELSE 0
        END
    ) AS previsao_vencida,
    SUM(ISNULL([Valor Frete], 0)) AS valor_frete_em_carteira,
    SUM(COALESCE(TRY_CONVERT(DECIMAL(18, 3), REPLACE([Peso Taxado], ',', '.')), 0)) AS peso_taxado_total,
    CAST(
        100.0 * SUM(CASE WHEN [Status Carga] = 'Finalizado' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS pct_finalizado,
    MAX([Data de extracao]) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'manifestos',
    label: 'Manifestos',
    apiPath: '/api/painel/manifestos',
    uiPath: '/manifestos',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'total_manifestos', label: 'Total Manifestos', uiLabel: 'Total Manifestos', sqlKey: 'total_manifestos', apiKey: 'totalManifestos', type: 'count', critical: true }),
      metric({ id: 'em_transito', label: 'Em Trânsito', uiLabel: 'Em Trânsito', sqlKey: 'em_transito', apiKey: 'emTransito', type: 'count' }),
      metric({ id: 'encerrados', label: 'Encerrados', uiLabel: 'Encerrados', sqlKey: 'encerrados', apiKey: 'encerrados', type: 'count' }),
      metric({ id: 'km_total', label: 'KM Total', uiLabel: 'KM Total', sqlKey: 'km_total', apiKey: 'kmTotal', type: 'number', toleranceAbs: 0.01 }),
      metric({ id: 'custo_total', label: 'Custo Total', uiLabel: 'Custo Total', sqlKey: 'custo_total', apiKey: 'custoTotal', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'custo_por_km', label: 'Custo/KM', uiLabel: 'Custo/KM', sqlKey: 'custo_por_km', apiKey: 'custoPorKm', type: 'currency', toleranceAbs: 0.01 }),
      metric({ id: 'ocupacao_peso_media_pct', label: 'Ocup. Peso %', uiLabel: 'Ocup. Peso %', sqlKey: 'ocupacao_peso_media_pct', apiKey: 'ocupacaoPesoMediaPct', type: 'percentage', toleranceAbs: 0.5 }),
      metric({ id: 'ocupacao_cubagem_media_pct', label: 'Ocup. Cubagem %', uiLabel: 'Ocup. Cubagem %', sqlKey: 'ocupacao_cubagem_media_pct', apiKey: 'ocupacaoCubagemMediaPct', type: 'percentage', toleranceAbs: 0.5 }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_manifestos_powerbi
    WHERE [Data criação] >= @DataInicioOffset
      AND [Data criação] < @DataFimExclusivoOffset
)`,
      select: `
SELECT
    COUNT(*) AS total_manifestos,
    SUM(CASE WHEN [Status] IN ('em trânsito', 'em transito') THEN 1 ELSE 0 END) AS em_transito,
    SUM(CASE WHEN [Status] = 'encerrado' THEN 1 ELSE 0 END) AS encerrados,
    SUM(ISNULL([KM Total], 0)) AS km_total,
    SUM(ISNULL([Custo total], 0)) AS custo_total,
    CAST(
        SUM(ISNULL([Custo total], 0)) / NULLIF(SUM(ISNULL([KM Total], 0)), 0)
        AS DECIMAL(18, 2)
    ) AS custo_por_km,
    CAST(AVG(
        CASE
            WHEN ISNULL([Capacidade Lotação Kg], 0) > 0
            THEN [Total peso taxado] * 100.0 / [Capacidade Lotação Kg]
        END
    ) AS DECIMAL(10, 2)) AS ocupacao_peso_media_pct,
    CAST(AVG(
        CASE
            WHEN ISNULL([Veículo/Peso Cubado], 0) > 0
            THEN [Total M3] * 100.0 / [Veículo/Peso Cubado]
        END
    ) AS DECIMAL(10, 2)) AS ocupacao_cubagem_media_pct,
    MAX([Data de extracao]) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'faturas_por_cliente',
    label: 'Faturas por Cliente',
    apiPath: '/api/painel/faturas-por-cliente',
    uiPath: '/faturas-por-cliente',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: true },
    metrics: [
      metric({ id: 'valor_faturado', label: 'Valor Faturado', uiLabel: 'Valor Faturado', sqlKey: 'valor_faturado', apiKey: 'valorFaturado', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'registros_faturados', label: 'Reg. Faturados', uiLabel: 'Reg. Faturados', sqlKey: 'registros_faturados', apiKey: 'registrosFaturados', type: 'count' }),
      metric({ id: 'aguardando_faturamento', label: 'Ag. Faturamento', uiLabel: 'Ag. Faturamento', sqlKey: 'aguardando_faturamento', apiKey: 'aguardandoFaturamento', type: 'count' }),
      metric({ id: 'titulos_em_atraso', label: 'Tít. Atraso', uiLabel: 'Tít. Atraso', sqlKey: 'titulos_em_atraso', apiKey: 'titulosEmAtraso', type: 'count', critical: true }),
      metric({ id: 'prazo_medio_dias', label: 'Prazo Médio', uiLabel: 'Prazo Médio', sqlKey: 'prazo_medio_dias', apiKey: 'prazoMedioDias', type: 'days', toleranceAbs: 0.1 }),
      metric({ id: 'clientes_ativos', label: 'Clientes Ativos', uiLabel: 'Clientes Ativos', sqlKey: 'clientes_ativos', apiKey: 'clientesAtivos', type: 'count' }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_faturas_por_cliente_powerbi
    WHERE [CT-e/Data de emissão] >= @DataInicioOffset
      AND [CT-e/Data de emissão] < @DataFimExclusivoOffset
),
normalizada AS (
    SELECT *
    FROM (
        SELECT
            b.*,
            ROW_NUMBER() OVER (
                PARTITION BY [ID Único]
                ORDER BY [Data da Última Atualização] DESC, [ID Único] ASC
            ) AS rn
        FROM base b
    ) x
    WHERE x.rn = 1
)`,
      select: `
SELECT
    SUM(
        CASE
            WHEN NULLIF(LTRIM(RTRIM([Fatura/N° Documento])), '') IS NOT NULL
            THEN COALESCE([Fatura/Valor], [Fatura/Valor Total], [Frete/Valor dos CT-es], 0)
            ELSE 0
        END
    ) AS valor_faturado,
    SUM(
        CASE
            WHEN NULLIF(LTRIM(RTRIM([Fatura/N° Documento])), '') IS NOT NULL
            THEN 1 ELSE 0
        END
    ) AS registros_faturados,
    SUM(
        CASE
            WHEN NULLIF(LTRIM(RTRIM([Fatura/N° Documento])), '') IS NULL
            THEN 1 ELSE 0
        END
    ) AS aguardando_faturamento,
    SUM(
        CASE
            WHEN NULLIF(LTRIM(RTRIM([Fatura/N° Documento])), '') IS NOT NULL
             AND [Parcelas/Vencimento] < @Hoje
             AND [Fatura/Baixa] IS NULL
            THEN 1 ELSE 0
        END
    ) AS titulos_em_atraso,
    CAST(
        AVG(
            CASE
                WHEN NULLIF(LTRIM(RTRIM([Fatura/N° Documento])), '') IS NOT NULL
                 AND COALESCE([Fatura/Emissão], [Fatura/Emissão Fatura]) IS NOT NULL
                 AND [Parcelas/Vencimento] IS NOT NULL
                THEN DATEDIFF(
                    DAY,
                    COALESCE([Fatura/Emissão], [Fatura/Emissão Fatura]),
                    [Parcelas/Vencimento]
                ) * 1.0
            END
        ) AS DECIMAL(10, 1)
    ) AS prazo_medio_dias,
    COUNT(DISTINCT NULLIF(LTRIM(RTRIM([Pagador do frete/Nome])), '')) AS clientes_ativos,
    MAX([Data da Última Atualização]) AS ultima_extracao
FROM normalizada`,
    }),
  },
  {
    key: 'etl_saude',
    label: 'ETL Saúde',
    apiPath: '/api/painel/etl-saude',
    uiPath: '/etl-saude',
    updateMapping: { sqlKey: 'ultima_extracao', apiKey: 'updatedAt', comparable: false },
    metrics: [
      metric({ id: 'tempo_medio_execucao_segundos', label: 'Tempo Médio (s)', uiLabel: 'Tempo Médio (s)', sqlKey: 'tempo_medio_execucao_segundos', apiKey: 'tempoMedioExecucaoSegundos', type: 'number', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'execucoes_com_erro', label: 'Com Erro', uiLabel: 'Com Erro', sqlKey: 'execucoes_com_erro', apiKey: 'execucoesComErro', type: 'count', critical: true }),
      metric({ id: 'total_execucoes', label: 'Total Execuções', uiLabel: 'Total Execuções', sqlKey: 'total_execucoes', apiKey: 'totalExecucoes', type: 'count' }),
      metric({ id: 'volume_processado_total', label: 'Vol. Processado', uiLabel: 'Vol. Processado', sqlKey: 'volume_processado_total', apiKey: 'volumeProcessadoTotal', type: 'number' }),
      metric({ id: 'taxa_sucesso_pct', label: 'Taxa Sucesso', uiLabel: 'Taxa Sucesso', sqlKey: 'taxa_sucesso_pct', apiKey: 'taxaSucesso', type: 'percentage', toleranceAbs: 0.5, critical: true }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH base AS (
    SELECT *
    FROM dbo.vw_bi_monitoramento
    WHERE [Data] BETWEEN @DataInicio AND @DataFim
)`,
      select: `
SELECT
    CAST(AVG(ISNULL([Duracao (s)], 0) * 1.0) AS DECIMAL(10, 2)) AS tempo_medio_execucao_segundos,
    SUM(CASE WHEN UPPER(ISNULL([Status], '')) <> 'SUCCESS' THEN 1 ELSE 0 END) AS execucoes_com_erro,
    COUNT(*) AS total_execucoes,
    SUM(ISNULL([Total Registros], 0)) AS volume_processado_total,
    CAST(
        100.0 * SUM(CASE WHEN UPPER(ISNULL([Status], '')) = 'SUCCESS' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_sucesso_pct,
    MAX(CAST([Fim] AS DATETIME2)) AS ultima_extracao
FROM base`,
    }),
  },
  {
    key: 'executivo',
    label: 'Executivo',
    apiPath: '/api/painel/executivo',
    uiPath: '/executivo',
    updateMapping: { sqlKey: null, apiKey: 'updatedAt', comparable: false },
    metrics: [
      metric({ id: 'receita_operacional', label: 'Rec. Operacional', uiLabel: 'Rec. Operacional', sqlKey: 'receita_operacional', apiKey: 'receitaOperacional', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'valor_faturado', label: 'Valor Faturado', uiLabel: 'Valor Faturado', sqlKey: 'valor_faturado', apiKey: 'valorFaturado', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'saldo_a_receber', label: 'A Receber', uiLabel: 'A Receber', sqlKey: 'saldo_a_receber', apiKey: 'saldoAReceber', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'saldo_a_pagar', label: 'A Pagar', uiLabel: 'A Pagar', sqlKey: 'saldo_a_pagar', apiKey: 'saldoAPagar', type: 'currency', toleranceAbs: 0.01, critical: true }),
      metric({ id: 'backlog_coletas', label: 'Backlog Coletas', uiLabel: 'Backlog Coletas', sqlKey: 'backlog_coletas', apiKey: 'backlogColetas', type: 'count', critical: true }),
      metric({ id: 'previsao_vencida', label: 'Previsão Vencida', uiLabel: 'Previsão Vencida', sqlKey: 'previsao_vencida', apiKey: 'cargasPrevisaoVencida', type: 'count', critical: true }),
      metric({ id: 'ocupacao_peso_media_pct', label: 'Ocup. Manifestos', uiLabel: 'Ocup. Manifestos', sqlKey: 'ocupacao_peso_media_pct', apiKey: 'ocupacaoMediaManifestos', type: 'percentage', toleranceAbs: 0.5 }),
    ],
    sql: (period) => buildSqlJsonQuery({
      period,
      cte: `
WITH fretes AS (
    SELECT
        SUM(ISNULL([Valor Total do Serviço], 0)) AS receita_operacional
    FROM dbo.vw_fretes_powerbi
    WHERE [Data frete] >= @DataInicioOffset
      AND [Data frete] < @DataFimExclusivoOffset
),
titulos AS (
    SELECT
        SUM(ISNULL([Valor], 0)) AS valor_faturado,
        SUM(ISNULL([Valor a Pagar], 0)) AS saldo_a_receber
    FROM dbo.vw_faturas_graphql_powerbi
    WHERE [Emissão] BETWEEN @DataInicio AND @DataFim
),
contas AS (
    SELECT
        SUM(ISNULL([Valor a pagar], 0)) - SUM(ISNULL([Valor pago], 0)) AS saldo_a_pagar
    FROM dbo.vw_contas_a_pagar_powerbi
    WHERE [Emissão] BETWEEN @DataInicio AND @DataFim
),
coletas_raw AS (
    SELECT *
    FROM dbo.vw_coletas_powerbi
    WHERE [Solicitacao] BETWEEN @DataInicio AND @DataFim
),
coletas AS (
    SELECT
        COUNT(*) AS total_coletas,
        SUM(CASE WHEN [Status] IN (N'Finalizada', N'Coletada') THEN 1 ELSE 0 END) AS finalizadas
    FROM (
        SELECT
            *,
            ROW_NUMBER() OVER (
                PARTITION BY [ID]
                ORDER BY [Solicitacao] DESC, [Numero Manifesto] DESC
            ) AS rn
        FROM coletas_raw
    ) c
    WHERE rn = 1
),
tracking AS (
    SELECT
        SUM(
            CASE
                WHEN CAST([Previsão Entrega/Previsão de entrega] AS DATE) < @Hoje
                 AND [Status Carga] <> 'Finalizado'
                THEN 1 ELSE 0
            END
        ) AS previsao_vencida
    FROM dbo.vw_localizacao_cargas_powerbi
    WHERE [Data do frete] >= @DataInicioOffset
      AND [Data do frete] < @DataFimExclusivoOffset
),
manifestos AS (
    SELECT
        CAST(AVG(
            CASE
                WHEN ISNULL([Capacidade Lotação Kg], 0) > 0
                THEN [Total peso taxado] * 100.0 / [Capacidade Lotação Kg]
            END
        ) AS DECIMAL(10, 2)) AS ocupacao_peso_media_pct
    FROM dbo.vw_manifestos_powerbi
    WHERE [Data criação] >= @DataInicioOffset
      AND [Data criação] < @DataFimExclusivoOffset
)`,
      select: `
SELECT
    f.receita_operacional,
    t.valor_faturado,
    t.saldo_a_receber,
    c.saldo_a_pagar,
    col.total_coletas - col.finalizadas AS backlog_coletas,
    tr.previsao_vencida,
    m.ocupacao_peso_media_pct
FROM fretes f
CROSS JOIN titulos t
CROSS JOIN contas c
CROSS JOIN coletas col
CROSS JOIN tracking tr
CROSS JOIN manifestos m`,
    }),
  },
];

export const ENTITY_MAP = new Map(ENTITIES.map((entity) => [entity.key, entity]));
