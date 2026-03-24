# Validacao manual das entidades do dashboard

Este documento consolida:

- o inventario das tabelas fisicas do banco;
- o mapa entre tabela base, view e endpoint da interface;
- queries prontas para comparar valor bruto vs valor exposto pela view/API;
- queries-resumo para bater os KPIs mostrados na interface.

Observacoes:

- Todas as consultas abaixo foram montadas para SQL Server.
- `executivo` nao tem tabela propria; a tela e composta pelos resultados de `fretes`, `faturas`, `contas_a_pagar`, `coletas`, `localizacao de cargas` e `manifestos`.
- As telas administrativas de acesso nao usam tabela SQL Server deste projeto como fonte principal do painel; este documento foca o banco de BI/ETL.

## 1. Inventario das tabelas fisicas

| Tabela | Categoria | Uso principal | View/API relacionada |
| --- | --- | --- | --- |
| `dbo.coletas` | Dominio | Base operacional de coletas | `vw_coletas_powerbi` / `/api/painel/coletas` |
| `dbo.fretes` | Dominio | Base operacional de fretes | `vw_fretes_powerbi` / `/api/painel/fretes` |
| `dbo.manifestos` | Dominio | Base operacional de manifestos | `vw_manifestos_powerbi` / `/api/painel/manifestos` |
| `dbo.cotacoes` | Dominio | Base operacional de cotacoes | `vw_cotacoes_powerbi` / `/api/painel/cotacoes` |
| `dbo.localizacao_cargas` | Dominio | Base operacional de localizacao de cargas | `vw_localizacao_cargas_powerbi` / `/api/painel/tracking` |
| `dbo.contas_a_pagar` | Financeiro | Contas a pagar | `vw_contas_a_pagar_powerbi` / `/api/painel/contas-a-pagar` |
| `dbo.faturas_por_cliente` | Faturamento | Operacional de faturamento por cliente | `vw_faturas_por_cliente_powerbi` / `/api/painel/faturas` |
| `dbo.faturas_graphql` | Financeiro | Titulos financeiros de faturamento | `vw_faturas_graphql_powerbi` / `/api/painel/faturas` |
| `dbo.dim_usuarios` | Dimensao | Enriquecimento de usuarios | `vw_dim_usuarios` / filtros de coletas |
| `dbo.sys_execution_history` | Monitoramento | Historico de execucoes ETL | `vw_bi_monitoramento` / `/api/painel/etl-saude` |
| `dbo.log_extracoes` | Auditoria | Log por entidade extraida | Apoio operacional |
| `dbo.page_audit` | Auditoria | Auditoria de paginacao e requests | Apoio operacional |
| `dbo.sys_auditoria_temp` | Auditoria | Auditoria temporaria de campos | Apoio operacional |
| `dbo.schema_migrations` | Controle | Historico de migrations aplicadas | Apoio operacional |

## 2. Mapa da interface para o banco

| Entidade/Tela | Tabela base | View usada pela API | Endpoint |
| --- | --- | --- | --- |
| Coletas | `coletas`, `manifestos`, `dim_usuarios` | `vw_coletas_powerbi` | `/api/painel/coletas` |
| Fretes | `fretes` | `vw_fretes_powerbi` | `/api/painel/fretes` |
| Faturas | `faturas_por_cliente`, `faturas_graphql` | `vw_faturas_por_cliente_powerbi`, `vw_faturas_graphql_powerbi` | `/api/painel/faturas` |
| Cotacoes | `cotacoes` | `vw_cotacoes_powerbi` | `/api/painel/cotacoes` |
| Contas a pagar | `contas_a_pagar` | `vw_contas_a_pagar_powerbi` | `/api/painel/contas-a-pagar` |
| Localizacao de Cargas | `localizacao_cargas` | `vw_localizacao_cargas_powerbi` | `/api/painel/tracking` |
| Manifestos | `manifestos` | `vw_manifestos_powerbi` | `/api/painel/manifestos` |
| ETL Saude | `sys_execution_history` | `vw_bi_monitoramento` | `/api/painel/etl-saude` |
| Executivo | composicao das entidades acima | sem view propria | `/api/painel/executivo` |

## 3. Parametros base

Use estes parametros no inicio da sessao do SSMS e reaproveite nas consultas abaixo.
Na data de referencia de `2026-03-23`, o dashboard usa por padrao a janela de `Ultimos 30 dias`, portanto a comparacao oficial com a UI deve usar datas absolutas `2026-02-21` a `2026-03-23`:

```sql
DECLARE @DataInicio DATE = '2026-02-21';
DECLARE @DataFim DATE = '2026-03-23';
DECLARE @Limite INT = 200;
DECLARE @Hoje DATE = CAST(GETDATE() AS DATE);
```

Se a validacao precisar reproduzir o fechamento mensal de faturas usado fora da UI, troque explicitamente para `2026-03-01` a `2026-03-31`. Esse recorte mensal gera os valores `294666.63 / 46471.65 / 294677.79 / 14.11 / 38.9 / 26` em faturas.

## 4. Descoberta rapida do schema atual

### 4.1. Listar tabelas existentes no banco

```sql
SELECT
    s.name AS schema_name,
    t.name AS table_name,
    SUM(CASE WHEN p.index_id IN (0, 1) THEN p.rows ELSE 0 END) AS row_count_aprox
FROM sys.tables t
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
LEFT JOIN sys.partitions p ON p.object_id = t.object_id
GROUP BY s.name, t.name
ORDER BY s.name, t.name;
```

### 4.2. Listar views existentes no banco

```sql
SELECT
    s.name AS schema_name,
    v.name AS view_name
FROM sys.views v
INNER JOIN sys.schemas s ON s.schema_id = v.schema_id
ORDER BY s.name, v.name;
```

### 4.3. Listar colunas de todas as tabelas e views

```sql
SELECT
    TABLE_SCHEMA,
    TABLE_NAME,
    COLUMN_NAME,
    ORDINAL_POSITION,
    DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dbo'
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

## 5. Queries por entidade

## 5.1. Coletas

### 5.1.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    c.id AS id_coleta,
    c.sequence_code AS raw_coleta,
    vw.[Coleta] AS view_coleta,
    c.request_date AS raw_solicitacao,
    vw.[Solicitacao] AS view_solicitacao,
    c.request_hour AS raw_hora_solicitacao,
    vw.[Hora (Solicitacao)] AS view_hora_solicitacao,
    c.service_date AS raw_agendamento,
    vw.[Agendamento] AS view_agendamento,
    c.finish_date AS raw_finalizacao,
    vw.[Finalizacao] AS view_finalizacao,
    c.status AS raw_status,
    vw.[Status] AS view_status,
    c.total_volumes AS raw_volumes,
    vw.[Volumes] AS view_volumes,
    c.total_weight AS raw_peso_real,
    vw.[Peso Real] AS view_peso_real,
    c.taxed_weight AS raw_peso_taxado,
    vw.[Peso Taxado] AS view_peso_taxado,
    c.total_value AS raw_valor_nf,
    vw.[Valor NF] AS view_valor_nf,
    m.sequence_code AS raw_numero_manifesto,
    vw.[Numero Manifesto] AS view_numero_manifesto,
    c.cliente_nome AS raw_cliente,
    vw.[Cliente] AS view_cliente,
    c.cliente_doc AS raw_cliente_doc,
    vw.[Cliente Doc] AS view_cliente_doc,
    c.cidade_coleta AS raw_cidade,
    vw.[Cidade] AS view_cidade,
    c.uf_coleta AS raw_uf,
    vw.[UF] AS view_uf,
    c.pick_region AS raw_regiao,
    vw.[Região da Coleta] AS view_regiao,
    c.filial_nome AS raw_filial,
    vw.[Filial] AS view_filial,
    c.usuario_nome AS raw_usuario,
    vw.[Usuario] AS view_usuario,
    c.cancellation_reason AS raw_motivo_cancelamento,
    vw.[Motivo Cancel.] AS view_motivo_cancelamento,
    c.cancellation_user_id AS raw_usuario_cancel_id,
    u_cancel.nome AS joined_usuario_cancel_nome,
    vw.[Usuario Cancel. Nome] AS view_usuario_cancel_nome,
    c.destroy_reason AS raw_motivo_exclusao,
    vw.[Motivo Exclusao] AS view_motivo_exclusao,
    c.destroy_user_id AS raw_usuario_exclusao_id,
    u_destroy.nome AS joined_usuario_exclusao_nome,
    vw.[Usuario Exclusao Nome] AS view_usuario_exclusao_nome,
    c.last_occurrence AS raw_ultima_ocorrencia,
    vw.[Última Ocorrência] AS view_ultima_ocorrencia,
    c.numero_tentativas AS raw_numero_tentativas,
    vw.[Nº Tentativas] AS view_numero_tentativas,
    c.metadata AS raw_metadata,
    vw.[Metadata] AS view_metadata,
    c.data_extracao AS raw_data_extracao,
    vw.[Data de extracao] AS view_data_extracao
FROM dbo.coletas c
LEFT JOIN dbo.manifestos m ON m.pick_sequence_code = c.sequence_code
LEFT JOIN dbo.dim_usuarios u_cancel ON u_cancel.user_id = c.cancellation_user_id
LEFT JOIN dbo.dim_usuarios u_destroy ON u_destroy.user_id = c.destroy_user_id
LEFT JOIN dbo.vw_coletas_powerbi vw ON vw.[ID] = c.id
WHERE c.request_date BETWEEN @DataInicio AND @DataFim
ORDER BY c.request_date DESC, c.sequence_code DESC;
```

### 5.1.2. Resumo para bater os cards da tela

```sql
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
)
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
FROM base;
```

## 5.2. Fretes

### 5.2.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    f.id AS id_frete,
    f.servico_em AS raw_data_frete,
    vw.[Data frete] AS view_data_frete,
    f.status AS raw_status,
    vw.[Status] AS view_status,
    f.filial_nome AS raw_filial,
    vw.[Filial] AS view_filial,
    f.pagador_nome AS raw_pagador,
    vw.[Pagador] AS view_pagador,
    f.remetente_nome AS raw_remetente,
    vw.[Remetente] AS view_remetente,
    f.destinatario_nome AS raw_destinatario,
    vw.[Destinatario] AS view_destinatario,
    f.origem_cidade AS raw_origem,
    vw.[Origem] AS view_origem,
    f.origem_uf AS raw_uf_origem,
    vw.[UF Origem] AS view_uf_origem,
    f.destino_cidade AS raw_destino,
    vw.[Destino] AS view_destino,
    f.destino_uf AS raw_uf_destino,
    vw.[UF Destino] AS view_uf_destino,
    f.valor_total AS raw_valor_total,
    vw.[Valor Total do Serviço] AS view_valor_total,
    f.subtotal AS raw_valor_frete,
    vw.[Valor Frete] AS view_valor_frete,
    f.taxed_weight AS raw_kg_taxado,
    vw.[Kg Taxado] AS view_kg_taxado,
    f.invoices_total_volumes AS raw_volumes,
    vw.[Volumes] AS view_volumes,
    f.data_previsao_entrega AS raw_previsao_entrega,
    vw.[Previsão de Entrega] AS view_previsao_entrega,
    f.cte_id AS raw_cte_id,
    vw.[CT-e ID] AS view_cte_id,
    f.numero_cte AS raw_numero_cte,
    vw.[Nº CT-e] AS view_numero_cte,
    f.nfse_number AS raw_nfse_numero,
    vw.[Nº NFS-e] AS view_nfse_numero,
    f.modal AS raw_modal,
    vw.[Modal] AS view_modal,
    f.tipo_frete AS raw_tipo_frete,
    vw.[Tipo Frete] AS view_tipo_frete,
    f.fiscal_tax_value AS raw_valor_icms,
    vw.[Valor ICMS] AS view_valor_icms,
    f.fiscal_pis_value AS raw_valor_pis,
    vw.[Valor PIS] AS view_valor_pis,
    f.fiscal_cofins_value AS raw_valor_cofins,
    vw.[Valor COFINS] AS view_valor_cofins,
    f.data_extracao AS raw_data_extracao,
    vw.[Data de extracao] AS view_data_extracao
FROM dbo.fretes f
LEFT JOIN dbo.vw_fretes_powerbi vw ON vw.[ID] = f.id
WHERE f.servico_em >= @DataInicio
  AND f.servico_em < DATEADD(DAY, 1, @DataFim)
ORDER BY f.servico_em DESC, f.id DESC;
```

### 5.2.2. Resumo para bater os cards da tela

```sql
WITH base AS (
    SELECT *
    FROM dbo.vw_fretes_powerbi
    WHERE [Data frete] >= @DataInicio
      AND [Data frete] < DATEADD(DAY, 1, @DataFim)
)
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
FROM base;
```

## 5.3. Faturas

### 5.3.1. Detalhe operacional + financeiro + views

```sql
SELECT TOP (@Limite)
    fpc.unique_id AS id_operacional,
    fpc.fit_ant_document AS raw_documento_titulo_operacional,
    vop.[Fatura/N° Documento] AS view_documento_titulo_operacional,
    fg.document AS raw_documento_titulo_financeiro,
    vfg.[Fatura/N° Documento] AS view_documento_titulo_financeiro,
    fpc.filial AS raw_filial,
    vop.[Filial] AS view_filial,
    fpc.pagador_nome AS raw_pagador,
    vop.[Pagador do frete/Nome] AS view_pagador,
    fpc.numero_cte AS raw_numero_cte,
    vop.[CT-e/Número] AS view_numero_cte,
    fpc.data_emissao_cte AS raw_data_emissao_cte,
    vop.[CT-e/Data de emissão] AS view_data_emissao_cte,
    fpc.data_emissao_fatura AS raw_data_emissao_fatura,
    vop.[Fatura/Emissão Fatura] AS view_data_emissao_fatura,
    fpc.data_vencimento_fatura AS raw_vencimento_operacional,
    vop.[Parcelas/Vencimento] AS view_vencimento_operacional,
    COALESCE(fpc.fit_ant_value, fpc.valor_fatura, fpc.valor_frete) AS raw_valor_operacional_dashboard,
    COALESCE(vop.[Fatura/Valor], vop.[Fatura/Valor Total], vop.[Frete/Valor dos CT-es]) AS view_valor_operacional_dashboard,
    fg.value AS raw_valor_financeiro,
    vfg.[Valor] AS view_valor_financeiro,
    fg.paid_value AS raw_valor_pago,
    vfg.[Valor Pago] AS view_valor_pago,
    fg.value_to_pay AS raw_valor_a_pagar,
    vfg.[Valor a Pagar] AS view_valor_a_pagar,
    fg.due_date AS raw_vencimento_financeiro,
    vfg.[Vencimento] AS view_vencimento_financeiro,
    CASE
        WHEN fpc.fit_ant_document IS NOT NULL THEN 'Faturado'
        ELSE 'Aguardando Faturamento'
    END AS raw_status_processo_dashboard,
    vop.[Status do Processo] AS view_status_processo_dashboard,
    fg.paid AS raw_pago_bit,
    vfg.[Pago] AS view_pago,
    fg.status AS raw_status_financeiro,
    vfg.[Status] AS view_status_financeiro,
    CASE
        WHEN fg.id IS NULL THEN 'sem-titulo'
        WHEN ABS(
            COALESCE(fpc.fit_ant_value, fpc.valor_fatura, fpc.valor_frete, CAST(0 AS DECIMAL(18, 2)))
            - COALESCE(fg.value, CAST(0 AS DECIMAL(18, 2)))
        ) <= 0.01 THEN 'conciliado'
        ELSE 'divergente'
    END AS status_reconciliacao_manual,
    fpc.data_extracao AS raw_data_extracao_operacional,
    fg.data_extracao AS raw_data_extracao_financeiro
FROM dbo.faturas_por_cliente fpc
LEFT JOIN dbo.faturas_graphql fg
    ON fg.document = fpc.fit_ant_document
LEFT JOIN dbo.vw_faturas_por_cliente_powerbi vop
    ON vop.[ID Único] = fpc.unique_id
LEFT JOIN dbo.vw_faturas_graphql_powerbi vfg
    ON vfg.[ID] = fg.id
WHERE fpc.data_emissao_cte >= @DataInicio
  AND fpc.data_emissao_cte < DATEADD(DAY, 1, @DataFim)
ORDER BY fpc.data_emissao_cte DESC, fpc.unique_id DESC;
```

### 5.3.2. Resumo para bater os cards da tela

```sql
WITH titulos AS (
    SELECT *
    FROM dbo.vw_faturas_graphql_powerbi
    WHERE [Emissão] BETWEEN @DataInicio AND @DataFim
),
operacional AS (
    SELECT *
    FROM dbo.vw_faturas_por_cliente_powerbi
    WHERE [CT-e/Data de emissão] >= @DataInicio
      AND [CT-e/Data de emissão] < DATEADD(DAY, 1, @DataFim)
)
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
    ) AS ultima_extracao
;
```

### 5.3.3. Diagnostico extra de duplicidade

Se a tela de faturas parecer inflada, rode tambem o script `database/validacao/029_verificar_duplicacao_faturas.sql`.

Situacao validada no ambiente live em `2026-03-23`:

- nao ha duplicidade ativa em `dbo.faturas_graphql` por `document` nem por `document + issue_date`;
- `vw_faturas_por_cliente_powerbi` continua com risco residual, porque o join operacional x financeiro usa `document` simples e pode inflar valores se a origem passar a repetir documentos.

Consulta rapida:

```sql
SELECT
    document,
    issue_date,
    COUNT(*) AS quantidade
FROM dbo.faturas_graphql
WHERE document IS NOT NULL
  AND issue_date IS NOT NULL
GROUP BY document, issue_date
HAVING COUNT(*) > 1
ORDER BY quantidade DESC, document;
```

## 5.4. Cotacoes

### 5.4.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    c.sequence_code AS raw_numero_cotacao,
    vw.[N° Cotação] AS view_numero_cotacao,
    c.requested_at AS raw_data_cotacao,
    vw.[Data Cotação] AS view_data_cotacao,
    c.branch_nickname AS raw_filial,
    vw.[Filial] AS view_filial,
    c.requester_name AS raw_solicitante,
    vw.[Solicitante] AS view_solicitante,
    c.customer_name AS raw_cliente_pagador,
    vw.[Cliente Pagador] AS view_cliente_pagador,
    COALESCE(c.customer_nickname, c.customer_name) AS raw_cliente_dashboard,
    vw.[Cliente] AS view_cliente_dashboard,
    c.origin_city AS raw_cidade_origem,
    vw.[Cidade Origem] AS view_cidade_origem,
    c.origin_state AS raw_uf_origem,
    vw.[UF Origem] AS view_uf_origem,
    c.destination_city AS raw_cidade_destino,
    vw.[Cidade Destino] AS view_cidade_destino,
    c.destination_state AS raw_uf_destino,
    vw.[UF Destino] AS view_uf_destino,
    c.volumes AS raw_volume,
    vw.[Volume] AS view_volume,
    c.real_weight AS raw_peso_real,
    vw.[Peso real] AS view_peso_real,
    c.taxed_weight AS raw_peso_taxado,
    vw.[Peso taxado] AS view_peso_taxado,
    c.invoices_value AS raw_valor_nf,
    vw.[Valor NF] AS view_valor_nf,
    c.total_value AS raw_valor_frete,
    vw.[Valor frete] AS view_valor_frete,
    c.price_table AS raw_tabela,
    vw.[Tabela] AS view_tabela,
    CASE
        WHEN c.cte_issued_at IS NOT NULL OR c.nfse_issued_at IS NOT NULL THEN 'Convertida'
        WHEN c.disapprove_comments IS NOT NULL AND LEN(c.disapprove_comments) > 0 THEN 'Reprovada'
        ELSE 'Pendente'
    END AS raw_status_conversao,
    vw.[Status Conversão] AS view_status_conversao,
    c.disapprove_comments AS raw_motivo_perda,
    vw.[Motivo Perda] AS view_motivo_perda,
    c.cte_issued_at AS raw_cte_emissao,
    vw.[CT-e/Data de emissão] AS view_cte_emissao,
    c.nfse_issued_at AS raw_nfse_emissao,
    vw.[Nfse/Data de emissão] AS view_nfse_emissao,
    c.data_extracao AS raw_data_extracao,
    vw.[Data de extracao] AS view_data_extracao
FROM dbo.cotacoes c
LEFT JOIN dbo.vw_cotacoes_powerbi vw
    ON vw.[N° Cotação] = c.sequence_code
WHERE c.requested_at >= @DataInicio
  AND c.requested_at < DATEADD(DAY, 1, @DataFim)
ORDER BY c.requested_at DESC, c.sequence_code DESC;
```

### 5.4.2. Resumo para bater os cards da tela

```sql
WITH base AS (
    SELECT *
    FROM dbo.vw_cotacoes_powerbi
    WHERE [Data Cotação] >= @DataInicio
      AND [Data Cotação] < DATEADD(DAY, 1, @DataFim)
)
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
FROM base;
```

## 5.5. Contas a pagar

### 5.5.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    c.sequence_code AS raw_lancamento,
    vw.[Lançamento a Pagar/N°] AS view_lancamento,
    c.document_number AS raw_documento,
    vw.[N° Documento] AS view_documento,
    c.issue_date AS raw_emissao,
    vw.[Emissão] AS view_emissao,
    c.tipo_lancamento AS raw_tipo,
    vw.[Tipo] AS view_tipo,
    c.valor_original AS raw_valor,
    vw.[Valor] AS view_valor,
    c.valor_juros AS raw_juros,
    vw.[Juros] AS view_juros,
    c.valor_desconto AS raw_desconto,
    vw.[Desconto] AS view_desconto,
    c.valor_a_pagar AS raw_valor_a_pagar,
    vw.[Valor a pagar] AS view_valor_a_pagar,
    c.valor_pago AS raw_valor_pago,
    vw.[Valor pago] AS view_valor_pago,
    c.nome_fornecedor AS raw_fornecedor,
    vw.[Fornecedor/Nome] AS view_fornecedor,
    c.nome_filial AS raw_filial,
    vw.[Filial] AS view_filial,
    c.classificacao_contabil AS raw_classificacao_contabil,
    vw.[Conta Contábil/Classificação] AS view_classificacao_contabil,
    c.descricao_contabil AS raw_descricao_contabil,
    vw.[Conta Contábil/Descrição] AS view_descricao_contabil,
    c.nome_centro_custo AS raw_centro_custo,
    vw.[Centro de custo/Nome] AS view_centro_custo,
    c.data_liquidacao AS raw_data_liquidacao,
    vw.[Baixa/Data liquidação] AS view_data_liquidacao,
    c.status_pagamento AS raw_status_pagamento,
    vw.[Status Pagamento] AS view_status_pagamento,
    CASE WHEN c.status_pagamento = 'PAGO' THEN 'Sim' ELSE 'Não' END AS raw_pago_dashboard,
    vw.[Pago] AS view_pago_dashboard,
    c.reconciliado AS raw_reconciliado_bit,
    vw.[Conciliado] AS view_conciliado,
    c.data_extracao AS raw_data_extracao,
    vw.[Data de extracao] AS view_data_extracao
FROM dbo.contas_a_pagar c
LEFT JOIN dbo.vw_contas_a_pagar_powerbi vw
    ON vw.[Lançamento a Pagar/N°] = c.sequence_code
WHERE c.issue_date BETWEEN @DataInicio AND @DataFim
ORDER BY c.issue_date DESC, c.sequence_code DESC;
```

### 5.5.2. Resumo para bater os cards da tela

```sql
WITH base AS (
    SELECT *
    FROM dbo.vw_contas_a_pagar_powerbi
    WHERE [Emissão] BETWEEN @DataInicio AND @DataFim
)
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
FROM base;
```

## 5.6. Localizacao de Cargas

### 5.6.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    l.sequence_number AS raw_minuta,
    vw.[N° Minuta] AS view_minuta,
    l.service_at AS raw_data_frete,
    vw.[Data do frete] AS view_data_frete,
    l.type AS raw_tipo,
    vw.[Tipo] AS view_tipo,
    l.invoices_volumes AS raw_volumes,
    vw.[Volumes] AS view_volumes,
    l.taxed_weight AS raw_peso_taxado,
    vw.[Peso Taxado] AS view_peso_taxado,
    l.invoices_value AS raw_valor_nf,
    vw.[Valor NF] AS view_valor_nf,
    l.total_value AS raw_valor_frete,
    vw.[Valor Frete] AS view_valor_frete,
    l.branch_nickname AS raw_filial_emissora,
    vw.[Filial Emissora] AS view_filial_emissora,
    l.origin_branch_nickname AS raw_filial_origem,
    vw.[Filial Origem] AS view_filial_origem,
    l.status_branch_nickname AS raw_filial_atual,
    vw.[Filial Atual] AS view_filial_atual,
    l.destination_branch_nickname AS raw_filial_destino,
    vw.[Filial Destino] AS view_filial_destino,
    l.origin_location_name AS raw_regiao_origem,
    vw.[Região Origem] AS view_regiao_origem,
    l.destination_location_name AS raw_regiao_destino,
    vw.[Região Destino] AS view_regiao_destino,
    l.classification AS raw_classificacao,
    vw.[Classificação] AS view_classificacao,
    l.status AS raw_status,
    vw.[Status Carga] AS view_status,
    l.predicted_delivery_at AS raw_previsao_entrega,
    vw.[Previsão Entrega/Previsão de entrega] AS view_previsao_entrega,
    l.data_extracao AS raw_data_extracao,
    vw.[Data de extracao] AS view_data_extracao
FROM dbo.localizacao_cargas l
LEFT JOIN dbo.vw_localizacao_cargas_powerbi vw
    ON vw.[N° Minuta] = l.sequence_number
WHERE l.service_at >= @DataInicio
  AND l.service_at < DATEADD(DAY, 1, @DataFim)
ORDER BY l.service_at DESC, l.sequence_number DESC;
```

### 5.6.2. Resumo para bater os cards da tela

```sql
WITH base AS (
    SELECT *
    FROM dbo.vw_localizacao_cargas_powerbi
    WHERE [Data do frete] >= @DataInicio
      AND [Data do frete] < DATEADD(DAY, 1, @DataFim)
)
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
FROM base;
```

## 5.7. Manifestos

### 5.7.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    m.id AS id_manifesto,
    m.sequence_code AS raw_numero_manifesto,
    vw.[Número] AS view_numero_manifesto,
    m.identificador_unico AS raw_identificador_unico,
    vw.[Identificador Único] AS view_identificador_unico,
    m.created_at AS raw_data_criacao,
    vw.[Data criação] AS view_data_criacao,
    m.status AS raw_status,
    vw.[Status] AS view_status,
    m.branch_nickname AS raw_filial,
    vw.[Filial] AS view_filial,
    m.driver_name AS raw_motorista,
    vw.[Motorista] AS view_motorista,
    m.vehicle_plate AS raw_placa,
    vw.[Veículo/Placa] AS view_placa,
    m.vehicle_type AS raw_tipo_veiculo,
    vw.[Tipo Veículo] AS view_tipo_veiculo,
    m.total_taxed_weight AS raw_total_peso_taxado,
    vw.[Total peso taxado] AS view_total_peso_taxado,
    m.total_cubic_volume AS raw_total_m3,
    vw.[Total M3] AS view_total_m3,
    m.total_cost AS raw_custo_total,
    vw.[Custo total] AS view_custo_total,
    m.freight_subtotal AS raw_valor_frete,
    vw.[Valor frete] AS view_valor_frete,
    m.fuel_subtotal AS raw_combustivel,
    vw.[Combustível] AS view_combustivel,
    m.toll_subtotal AS raw_pedagio,
    vw.[Pedágio] AS view_pedagio,
    m.paying_total AS raw_saldo_pagar,
    vw.[Saldo a pagar] AS view_saldo_pagar,
    m.km AS raw_km_total,
    vw.[KM Total] AS view_km_total,
    m.manifest_items_count AS raw_itens_total,
    vw.[Itens/Total] AS view_itens_total,
    m.capacidade_kg AS raw_capacidade_kg,
    vw.[Capacidade Lotação Kg] AS view_capacidade_kg,
    m.vehicle_cubic_weight AS raw_veiculo_peso_cubado,
    vw.[Veículo/Peso Cubado] AS view_veiculo_peso_cubado,
    m.data_extracao AS raw_data_extracao,
    vw.[Data de extracao] AS view_data_extracao
FROM dbo.manifestos m
LEFT JOIN dbo.vw_manifestos_powerbi vw
    ON vw.[Número] = m.sequence_code
   AND vw.[Data criação] = m.created_at
   AND ISNULL(vw.[MDF-es/Chave], '') = ISNULL(m.mdfe_key, '')
   AND ISNULL(vw.[Coleta/Número], -1) = ISNULL(m.pick_sequence_code, -1)
WHERE m.created_at >= @DataInicio
  AND m.created_at < DATEADD(DAY, 1, @DataFim)
ORDER BY m.created_at DESC, m.sequence_code DESC, m.id DESC;
```

### 5.7.2. Resumo para bater os cards da tela

```sql
WITH base AS (
    SELECT *
    FROM dbo.vw_manifestos_powerbi
    WHERE [Data criação] >= @DataInicio
      AND [Data criação] < DATEADD(DAY, 1, @DataFim)
)
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
FROM base;
```

## 5.8. ETL Saude

### 5.8.1. Detalhe bruto vs view

```sql
SELECT TOP (@Limite)
    s.id AS raw_id,
    vw.[Id] AS view_id,
    s.start_time AS raw_inicio,
    vw.[Inicio] AS view_inicio,
    s.end_time AS raw_fim,
    vw.[Fim] AS view_fim,
    s.duration_seconds AS raw_duracao_segundos,
    vw.[Duracao (s)] AS view_duracao_segundos,
    s.status AS raw_status,
    vw.[Status] AS view_status,
    s.total_records AS raw_total_registros,
    vw.[Total Registros] AS view_total_registros,
    s.error_category AS raw_categoria_erro,
    vw.[Categoria Erro] AS view_categoria_erro,
    s.error_message AS raw_mensagem_erro,
    vw.[Mensagem Erro] AS view_mensagem_erro
FROM dbo.sys_execution_history s
LEFT JOIN dbo.vw_bi_monitoramento vw
    ON vw.[Id] = s.id
WHERE CAST(s.start_time AS DATE) BETWEEN @DataInicio AND @DataFim
ORDER BY s.start_time DESC, s.id DESC;
```

### 5.8.2. Resumo para bater os cards da tela

```sql
WITH base AS (
    SELECT *
    FROM dbo.vw_bi_monitoramento
    WHERE [Data] BETWEEN @DataInicio AND @DataFim
)
SELECT
    CAST(AVG(ISNULL([Duracao (s)], 0) * 1.0) AS DECIMAL(10, 2)) AS tempo_medio_execucao_segundos,
    SUM(CASE WHEN UPPER(ISNULL([Status], '')) <> 'SUCCESS' THEN 1 ELSE 0 END) AS execucoes_com_erro,
    COUNT(*) AS total_execucoes,
    SUM(ISNULL([Total Registros], 0)) AS volume_processado_total,
    CAST(
        100.0 * SUM(CASE WHEN UPPER(ISNULL([Status], '')) = 'SUCCESS' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
        AS DECIMAL(10, 2)
    ) AS taxa_sucesso_pct
FROM base;
```

## 5.9. Executivo

### 5.9.1. Resumo consolidado para bater os cards da tela

```sql
WITH fretes AS (
    SELECT
        SUM(ISNULL([Valor Total do Serviço], 0)) AS receita_operacional
    FROM dbo.vw_fretes_powerbi
    WHERE [Data frete] >= @DataInicio
      AND [Data frete] < DATEADD(DAY, 1, @DataFim)
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
    WHERE [Data do frete] >= @DataInicio
      AND [Data do frete] < DATEADD(DAY, 1, @DataFim)
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
    WHERE [Data criação] >= @DataInicio
      AND [Data criação] < DATEADD(DAY, 1, @DataFim)
)
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
CROSS JOIN manifestos m;
```

## 6. Dimensoes e filtros da UI

Para garantir que os filtros da interface tragam todas as opcoes corretas, rode tambem o script `database/validacao/025_validar_views_dimensao.sql`.

### 6.1. Filiais

```sql
SELECT TOP (@Limite)
    [NomeFilial]
FROM dbo.vw_dim_filiais
ORDER BY [NomeFilial];
```

### 6.2. Clientes

```sql
SELECT TOP (@Limite)
    [Nome]
FROM dbo.vw_dim_clientes
ORDER BY [Nome];
```

### 6.3. Veiculos

```sql
SELECT TOP (@Limite)
    Placa,
    TipoVeiculo,
    Proprietario
FROM dbo.vw_dim_veiculos
ORDER BY Placa;
```

### 6.4. Motoristas

```sql
SELECT TOP (@Limite)
    NomeMotorista
FROM dbo.vw_dim_motoristas
ORDER BY NomeMotorista;
```

### 6.5. Plano de contas

```sql
SELECT TOP (@Limite)
    Descricao,
    Classificacao
FROM dbo.vw_dim_planocontas
ORDER BY Descricao;
```

### 6.6. Usuarios

```sql
SELECT TOP (@Limite)
    du.user_id AS raw_user_id,
    vw.[User ID] AS view_user_id,
    du.nome AS raw_nome,
    vw.[Nome] AS view_nome,
    du.data_atualizacao AS raw_data_atualizacao,
    vw.[Data Atualizacao] AS view_data_atualizacao
FROM dbo.dim_usuarios du
LEFT JOIN dbo.vw_dim_usuarios vw
    ON vw.[User ID] = du.user_id
ORDER BY du.data_atualizacao DESC, du.user_id DESC;
```

## 7. Tabelas tecnicas e auditoria

## 7.1. Logs de extracao

```sql
SELECT TOP (@Limite)
    id,
    entidade,
    timestamp_inicio,
    timestamp_fim,
    status_final,
    registros_extraidos,
    paginas_processadas,
    mensagem
FROM dbo.log_extracoes
ORDER BY timestamp_fim DESC, id DESC;
```

## 7.2. Auditoria de paginacao

```sql
SELECT TOP (@Limite)
    id,
    execution_uuid,
    run_uuid,
    template_id,
    page,
    per,
    janela_inicio,
    janela_fim,
    total_itens,
    status_code,
    duracao_ms,
    [timestamp]
FROM dbo.page_audit
ORDER BY [timestamp] DESC, id DESC;
```

## 7.3. Auditoria temporaria de campos

```sql
SELECT TOP (@Limite)
    entidade,
    campo_api,
    data_auditoria
FROM dbo.sys_auditoria_temp
ORDER BY data_auditoria DESC;
```

## 7.4. Migrations aplicadas

```sql
SELECT TOP (@Limite)
    migration_id,
    applied_at,
    checksum_sha256,
    notes
FROM dbo.schema_migrations
ORDER BY applied_at DESC, migration_id DESC;
```

## 8. Checagens rapidas adicionais

## 8.1. Contagem por entidade nas ultimas 24 horas

```sql
SELECT
    'coletas' AS entidade,
    COUNT(*) AS total_registros,
    MAX(data_extracao) AS ultima_extracao
FROM dbo.coletas
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'fretes',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.fretes
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'manifestos',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.manifestos
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'cotacoes',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.cotacoes
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'localizacao_cargas',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.localizacao_cargas
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'contas_a_pagar',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.contas_a_pagar
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'faturas_por_cliente',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.faturas_por_cliente
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'faturas_graphql',
    COUNT(*),
    MAX(data_extracao)
FROM dbo.faturas_graphql
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())

UNION ALL

SELECT
    'dim_usuarios',
    COUNT(*),
    MAX(data_atualizacao)
FROM dbo.dim_usuarios

UNION ALL

SELECT
    'sys_execution_history',
    COUNT(*),
    MAX(end_time)
FROM dbo.sys_execution_history
WHERE end_time >= DATEADD(HOUR, -24, GETDATE())

ORDER BY entidade;
```

## 8.2. API vs banco em uma linha

Se quiser uma validacao rapida de volume, reaproveite tambem o script `database/validacao/030_api_vs_banco_uma_linha.sql`.

## 9. Sugestao pratica de uso

Para validar uma tela manualmente, o fluxo mais seguro e:

1. Rodar a query detalhe da entidade com o mesmo periodo da interface.
2. Filtrar por uma chave visivel na UI: numero, documento, minuta, coleta, manifesto ou cliente.
3. Conferir bruto vs view na mesma linha.
4. Rodar a query resumo da entidade e bater os cards/KPIs da tela.
5. Se a divergencia for em filtro, rodar a query da dimensao correspondente.
6. Se a divergencia for em volume, rodar as checagens de `log_extracoes`, `page_audit` e `sys_execution_history`.
