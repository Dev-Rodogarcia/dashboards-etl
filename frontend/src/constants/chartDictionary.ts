export interface ChartDefinition {
  descricao: string;
  tabelasOrigem: string;
  cruzamentos: string;
  calculoTecnico: string;
  calculoNegocio: string;
  agrupamento: string;
}

const fretesFaturamentoBase = {
  tabelasOrigem: 'dbo.fato_fretes_faturamento',
  cruzamentos: 'Nenhum JOIN; CTE fretes -> filtrados no VisaoFretesRepository.',
} as const;

const cotacoesBase = {
  tabelasOrigem: 'vw_cotacoes_powerbi',
  cruzamentos: 'Nenhum JOIN; CTE base_filtrada -> base_deduplicada por ROW_NUMBER() PARTITION BY [N° Cotação].',
} as const;

const coletasBase = {
  tabelasOrigem: 'vw_coletas_powerbi',
  cruzamentos: 'Nenhum JOIN; CTE base_filtrada -> base_deduplicada por ROW_NUMBER() PARTITION BY [ID].',
} as const;

const trackingBase = {
  tabelasOrigem: 'vw_localizacao_cargas_powerbi',
  cruzamentos: 'Nenhum JOIN nas queries dos gráficos; a projeção TRACKING usa OUTER APPLY para normalizar status, filial e responsável.',
} as const;

const performanceBase = {
  tabelasOrigem: 'dbo.vw_fretes_powerbi',
  cruzamentos: 'Nenhum JOIN; CTE fonte -> entregas com ROW_NUMBER() PARTITION BY [Nº Minuta].',
} as const;

const faturasClienteBase = {
  tabelasOrigem: 'dbo.fato_gestao_vista_faturas',
  cruzamentos: 'Nenhum JOIN; CTE base_normalizada no FaturasPorClienteSqlRepository.',
} as const;

const contasPagarBase = {
  tabelasOrigem: 'vw_contas_a_pagar_powerbi',
  cruzamentos: 'Nenhum JOIN; leitura via DashboardExportDefinition.CONTAS_A_PAGAR.',
} as const;

const integracoesBase = {
  tabelasOrigem: 'Satélite TMS / endpoints de auditoria de integrações',
  cruzamentos: 'Sem JOIN no Dashboard; dados consolidados no backend Satélite e repassados pelo proxy /api/painel/integracoes.',
} as const;

export const chartDictionary = {
  coletasSerie: {
    ...coletasBase,
    descricao: 'Acompanha o volume diário de solicitações de coleta e separa o que avançou, foi cancelado ou permaneceu em tratativa no período filtrado.',
    calculoTecnico:
      "COUNT(1); SUM(CASE WHEN status_normalizado IN (N'finalizada', N'coletada') THEN 1 ELSE 0 END); SUM(CASE WHEN status_normalizado = N'cancelada' THEN 1 ELSE 0 END); SUM(CASE WHEN status_normalizado = N'em tratativa' THEN 1 ELSE 0 END)",
    calculoNegocio:
      'Conta cada coleta deduplicada do dia e distribui esse total pelas situações operacionais, permitindo enxergar demanda, conclusão, cancelamento e pendência na mesma linha do tempo. O gráfico inclui uma linha tracejada de marcação dinâmica (markLine) que exibe a média aritmética do total de coletas do período selecionado.',
    agrupamento: 'GROUP BY data_solicitacao',
  },
  coletasStatus: {
    ...coletasBase,
    descricao: 'Mostra a distribuição atual das coletas por status para evidenciar concentração de pendências, conclusões e exceções operacionais.',
    calculoTecnico: 'COUNT(1)',
    calculoNegocio:
      'Cada coleta deduplicada vale uma unidade e é posicionada no status normalizado correspondente; status vazio entra como "Sem status" para não desaparecer do gráfico.',
    agrupamento: "GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status]))), N''), N'Sem status')",
  },
  coletasHistoricoPerformance: {
    ...coletasBase,
    descricao: 'Evolui o percentual de coletas realizadas dentro do prazo, dando visão histórica da disciplina operacional de atendimento.',
    calculoTecnico:
      'CAST(COALESCE(CAST(no_prazo AS FLOAT) * 100.0 / NULLIF(no_prazo + fora_do_prazo, 0), 0) AS DECIMAL(19,2))',
    calculoNegocio:
      'Divide as coletas no prazo pelo total de coletas avaliáveis, ignorando divisão por zero, e apresenta o resultado como percentual de cumprimento do prazo.',
    agrupamento: 'GROUP BY data_solicitacao ou CAST(DATEADD(month, DATEDIFF(month, 0, data_solicitacao), 0) AS date)',
  },
  coletasOrigem: {
    ...coletasBase,
    descricao: 'Classifica as coletas pela região ou cidade de origem para revelar onde a demanda nasce e qual peso operacional cada localidade gera.',
    calculoTecnico: 'COUNT(DISTINCT [Coleta]); SUM(COALESCE([Peso Taxado], 0))',
    calculoNegocio:
      'Conta coletas únicas por localidade e soma o peso taxado associado, combinando volume de solicitações com impacto físico de carga por origem.',
    agrupamento:
      "GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Região da Coleta]))), N''), N'Sem mapeamento') ou GROUP BY cidade normalizada no drill",
  },
  coletasAging: {
    ...coletasBase,
    descricao: 'Segmenta coletas em aberto por idade para priorizar tratativas antigas e reduzir risco de atraso operacional.',
    calculoTecnico: 'COUNT(DISTINCT [Coleta])',
    calculoNegocio:
      'Conta cada coleta aberta uma única vez e a coloca na faixa de dias decorridos desde a solicitação até a data de referência.',
    agrupamento:
      "GROUP BY CASE WHEN DATEDIFF(day, [Solicitacao], :dataReferencia) <= 2 THEN N'0-2 dias' WHEN <= 5 THEN N'3-5 dias' WHEN <= 10 THEN N'6-10 dias' ELSE N'11+ dias' END",
  },

  manifestosCustoEvolucao: {
    tabelasOrigem: 'dbo.vw_fato_manifestos_dash, dbo.dim_calendario',
    cruzamentos: 'Nenhum JOIN no gráfico; dbo.dim_calendario é consultada separadamente para metas/dias úteis.',
    descricao: 'Evolui o custo total dos manifestos ao longo do tempo para acompanhar pressão de frete, contratação e execução logística.',
    calculoTecnico: 'COALESCE(SUM(custo_total), 0)',
    calculoNegocio:
      'Soma todos os custos de manifestos criados em cada data e retorna zero quando não há valor, mantendo a série financeira contínua.',
    agrupamento: 'GROUP BY data_criacao_date',
  },
  manifestosStatusTemporal: {
    tabelasOrigem: 'dbo.vw_fato_manifestos_dash',
    cruzamentos: 'Nenhum JOIN; CTE manifestos no ManifestosPerformanceSqlRepository.',
    descricao: 'Mostra a evolução dos manifestos por situação operacional para identificar acúmulo de pendentes, trânsito e encerramentos.',
    calculoTecnico:
      "SUM(CASE WHEN status_norm = N'Encerrado' THEN 1 ELSE 0 END); SUM(CASE WHEN status_norm = N'Em Trânsito' THEN 1 ELSE 0 END); SUM(CASE WHEN status_norm = N'Pendente' THEN 1 ELSE 0 END)",
    calculoNegocio:
      'Conta manifestos por período e reparte o volume entre encerrados, em trânsito e pendentes, permitindo comparar execução concluída contra carteira ainda aberta.',
    agrupamento:
      'GROUP BY data_criacao_date ou DATEFROMPARTS(YEAR(data_criacao_date), MONTH(data_criacao_date), 1) ou DATEFROMPARTS(YEAR(data_criacao_date), 1, 1)',
  },
  manifestosCustosContrato: {
    tabelasOrigem: 'dbo.vw_fato_manifestos_dash',
    cruzamentos: 'Nenhum JOIN; CTE manifestos no ManifestosPerformanceSqlRepository.',
    descricao: 'Compara o custo total dos manifestos por tipo de contrato para apoiar leitura de concentração e eficiência contratual.',
    calculoTecnico: 'COALESCE(SUM(custo_total), 0)',
    calculoNegocio:
      'Soma o custo dos manifestos dentro de cada tipo de contrato, mostrando quais modalidades concentram maior desembolso operacional. Veículos de propriedade da LM Transportes são reclassificados como Frota + PX. Casos sem motorista informado recebem fallback para Terceiro ou Frota + PX dependendo da tração.',
    agrupamento: 'GROUP BY tipo_contrato',
  },
  manifestosTiposVeiculo: {
    tabelasOrigem: 'dbo.vw_fato_manifestos_dash',
    cruzamentos: 'Nenhum JOIN; CTE manifestos no ManifestosPerformanceSqlRepository.',
    descricao: 'Distribui manifestos por tipo de veículo para entender o perfil de frota usado nas operações do período.',
    calculoTecnico: 'COUNT(DISTINCT sequence_code)',
    calculoNegocio:
      'Conta manifestos únicos por categoria de veículo, evitando duplicidade do mesmo manifesto e destacando a composição física da malha.',
    agrupamento: 'GROUP BY tipo_veiculo',
  },

  fretesReceitaDia: {
    ...fretesFaturamentoBase,
    descricao: 'Evolui a receita operacional dos fretes por data de referência para acompanhar geração diária de faturamento.',
    calculoTecnico: 'COALESCE(SUM(valor_total), 0)',
    calculoNegocio:
      'Soma o valor total dos fretes em cada dia do período filtrado e usa zero quando não há receita, mantendo a curva financeira legível.',
    agrupamento: 'GROUP BY data_referencia_periodo',
  },
  fretesTopClientes: {
    ...fretesFaturamentoBase,
    descricao: 'Ranqueia clientes pagadores por receita e volume elegível para destacar concentração comercial e ticket médio operacional.',
    calculoTecnico:
      'COALESCE(SUM(valor_total), 0); CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT); COALESCE(SUM(valor_total) / NULLIF(SUM(elegivel_faturamento), 0), 0)',
    calculoNegocio:
      'Para cada cliente, soma a receita, conta fretes elegíveis e divide receita por quantidade elegível para estimar o valor médio gerado.',
    agrupamento: 'GROUP BY pagador_nome',
  },
  fretesMixDocumental: {
    ...fretesFaturamentoBase,
    descricao: 'Mostra o mix documental dos fretes entre CT-e, NFS-e e pendentes para avaliar maturidade e pendências de emissão.',
    calculoTecnico: 'COUNT_BIG(1)',
    calculoNegocio:
      'Conta cada registro de frete e o classifica pelo documento disponível: CT-e quando há identificador de CT-e, NFS-e quando há número de nota de serviço, ou pendente.',
    agrupamento: "GROUP BY CASE WHEN cte_id IS NOT NULL THEN N'CT-e' WHEN nfse_numero IS NOT NULL THEN N'NFS-e' ELSE N'Pendente' END",
  },

  evolucaoFaturamento: {
    ...fretesFaturamentoBase,
    descricao: 'Acompanha a evolução de receita, subtotal e quantidade elegível para medir a cadência de faturamento operacional.',
    calculoTecnico:
      'COALESCE(SUM(valor_total), 0); COALESCE(SUM(subtotal), 0); CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT)',
    calculoNegocio:
      'Soma os valores financeiros dos fretes por período e contabiliza quantos itens entram na base elegível de faturamento.',
    agrupamento: 'GROUP BY data_referencia_periodo',
  },
  faturamentoClassificacao: {
    ...fretesFaturamentoBase,
    descricao: 'Quebra o faturamento por classificação operacional para comparar o peso financeiro de FTL, LTL, PTL e demais categorias.',
    calculoTecnico: 'COALESCE(SUM(valor_total), 0); CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT)',
    calculoNegocio:
      'Agrupa fretes pela classificação normalizada, soma a receita de cada grupo e conta o volume elegível para leitura de participação por tipo de operação.',
    agrupamento:
      "GROUP BY CASE WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%FTL%' THEN N'FTL' WHEN LIKE N'%LTL%' THEN N'LTL' WHEN LIKE N'%PTL%' THEN N'PTL' ELSE COALESCE(classificacao_nome, N'Sem classificação') END",
  },
  faturamentoResponsavel: {
    ...fretesFaturamentoBase,
    descricao: 'Distribui faturamento por responsável de destino para comparar carteira financeira e volume sob cada responsável regional.',
    calculoTecnico: 'COALESCE(SUM(valor_total), 0); CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT)',
    calculoNegocio:
      'Soma a receita e conta fretes elegíveis atribuídos a cada responsável de destino, usando rótulo padrão quando o responsável não foi informado.',
    agrupamento: "GROUP BY COALESCE(responsavel_regiao_destino, N'Responsável não informado')",
  },
  faturamentoParticipacaoClientes: {
    ...fretesFaturamentoBase,
    descricao: 'Mede a participação dos clientes no faturamento para identificar concentração de receita e dependência comercial.',
    calculoTecnico:
      'COALESCE(SUM(valor_total), 0); CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT); participação no frontend = receita do cliente / receita total do período',
    calculoNegocio:
      'Soma a receita por cliente, conta fretes elegíveis e compara a receita de cada cliente contra o total filtrado para calcular sua fatia no período.',
    agrupamento: 'GROUP BY pagador_nome',
  },
  faturamentoRota: {
    ...fretesFaturamentoBase,
    descricao: 'Aponta as rotas com maior faturamento por combinação de origem e destino, ajudando a entender corredores de receita.',
    calculoTecnico: 'COALESCE(SUM(valor_total), 0); CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT)',
    calculoNegocio:
      'Soma receita e volume elegível por par de UFs de origem e destino, substituindo ausência de UF por N/A para manter a rota visível.',
    agrupamento: "GROUP BY COALESCE(origem_uf, N'N/A'), COALESCE(destino_uf, N'N/A')",
  },

  performanceSerieTemporal: {
    ...performanceBase,
    descricao: 'Evolui entregas por status ao longo do prazo previsto para acompanhar finalizações, trânsito, pendências, cancelamentos e tratativas.',
    calculoTecnico:
      "COUNT_BIG(1); SUM(CASE WHEN status_norm = N'Finalizada' THEN 1 ELSE 0 END); SUM por Em Trânsito, Pendente, Cancelada e Em Tratativa",
    calculoNegocio:
      'Conta as entregas deduplicadas de cada período e distribui o total entre os status normalizados, expondo a composição da carteira logística.',
    agrupamento:
      'GROUP BY data_previsao_entrega ou DATEFROMPARTS(YEAR(data_previsao_entrega), MONTH(data_previsao_entrega), 1) ou DATEFROMPARTS(YEAR(data_previsao_entrega), 1, 1)',
  },
  performanceStatus: {
    ...performanceBase,
    descricao: 'Mostra a fotografia das entregas por status para evidenciar o tamanho da operação encerrada, ativa e problemática.',
    calculoTecnico: 'COUNT_BIG(1)',
    calculoNegocio:
      'Conta cada entrega deduplicada dentro do status normalizado correspondente, servindo como visão rápida da carteira operacional.',
    agrupamento: 'GROUP BY status_norm',
  },
  performanceHistorico: {
    ...performanceBase,
    descricao: 'Acompanha o histórico de cumprimento de prazo das entregas finalizadas para medir evolução de performance logística.',
    calculoTecnico:
      "SUM(CASE WHEN status_norm = N'Finalizada' THEN 1 ELSE 0 END); SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias <= 0 THEN 1 ELSE 0 END); percentual Java = noPrazo / finalizadas",
    calculoNegocio:
      'Conta entregas finalizadas, identifica quantas terminaram sem atraso e divide esse volume pelo total finalizado para obter o percentual no prazo.',
    agrupamento: 'GROUP BY CONVERT(char(7), data_previsao_entrega, 23)',
  },
  performanceDrilldown: {
    ...performanceBase,
    descricao: 'Detalha performance por responsável, região ou cidade para localizar onde estão pontualidade, atraso e risco operacional.',
    calculoTecnico:
      "SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias <= 0 THEN 1 ELSE 0 END); SUM(fora_do_prazo); SUM(em_atraso); COUNT_BIG(1)",
    calculoNegocio:
      'Para cada dimensão do drilldown, separa entregas no prazo, fora do prazo, ainda em atraso e volume total, permitindo comparar gargalos por área.',
    agrupamento: 'GROUP BY responsavel ou GROUP BY regiao_destino ou GROUP BY cidade_destino',
  },
  performanceAging: {
    ...performanceBase,
    descricao: 'Classifica entregas abertas atrasadas por faixa de dias para priorizar cobranças e atuação operacional.',
    calculoTecnico: 'COUNT_BIG(1)',
    calculoNegocio:
      'Conta entregas em aberto conforme a distância entre a previsão de entrega e a data atual, separando a carteira por gravidade de atraso.',
    agrupamento:
      "GROUP BY CASE WHEN DATEDIFF(day, data_previsao_entrega, :hoje) <= 2 THEN '0-2 dias' WHEN <= 5 THEN '3-5 dias' WHEN <= 10 THEN '6-10 dias' ELSE '11+ dias' END",
  },

  trackingStatus: {
    ...trackingBase,
    descricao: 'Resume cargas rastreadas por status de exibição e valor de frete para avaliar onde está o volume financeiro em trânsito.',
    calculoTecnico: 'COUNT(1); CAST(COALESCE(SUM(valor_frete), 0) AS DECIMAL(19,2))',
    calculoNegocio:
      'Conta as cargas em cada status de tracking e soma o valor de frete associado, conectando quantidade operacional com exposição financeira.',
    agrupamento: 'GROUP BY status_exibicao',
  },
  trackingValorRegiao: {
    ...trackingBase,
    descricao: 'Distribui valor de frete e quantidade de cargas por região de destino, destacando as regiões mais relevantes no tracking.',
    calculoTecnico: "CAST(COALESCE(SUM(valor_frete), 0) AS DECIMAL(19,2)); COUNT(1)",
    calculoNegocio:
      'Soma o valor dos fretes e conta cargas por região de destino; depois consolida regiões fora do Top 10 em "Outros" para manter leitura executiva.',
    agrupamento: "GROUP BY sigla_regiao_destino; depois GROUP BY CASE WHEN rn <= 10 THEN regiao ELSE N'Outros' END no Top 10",
  },

  faturasMensal: {
    ...faturasClienteBase,
    descricao: 'Evolui o valor operacional faturado e a quantidade de faturas por mês de referência.',
    calculoTecnico: 'CAST(COALESCE(SUM(valor_operacional), 0) AS DECIMAL(19,2)); COUNT(1)',
    calculoNegocio:
      'Soma o valor operacional das faturas de cada mês e conta quantos registros compõem esse total, permitindo enxergar tendência e densidade.',
    agrupamento: 'GROUP BY CONVERT(CHAR(7), data_referencia_mensal, 23)',
  },
  faturasAging: {
    ...faturasClienteBase,
    descricao: 'Agrupa faturas por faixa de aging para priorizar cobrança, regularização e acompanhamento financeiro.',
    calculoTecnico: 'CAST(COALESCE(SUM(valor_operacional), 0) AS DECIMAL(19,2)); COUNT(1)',
    calculoNegocio:
      'Soma o valor operacional e conta as faturas em cada faixa de vencimento ou atraso, mostrando concentração financeira por idade.',
    agrupamento: 'GROUP BY faixa',
  },
  faturasTopClientes: {
    tabelasOrigem: 'dbo.fato_gestao_vista_faturas',
    cruzamentos: 'JOIN representantes ON representantes.cliente_chave = totais.cliente_chave',
    descricao: 'Ranqueia clientes por valor faturado e quantidade de faturas, com vínculo ao representante responsável.',
    calculoTecnico: 'CAST(COALESCE(SUM(valor_operacional), 0) AS DECIMAL(19,2)); COUNT(1)',
    calculoNegocio:
      'Soma o valor operacional de cada cliente, conta suas faturas e associa a carteira ao representante para leitura comercial e financeira.',
    agrupamento: 'GROUP BY cliente_chave',
  },
  faturasStatusProcesso: {
    ...faturasClienteBase,
    descricao: 'Mostra faturas por status de processo para identificar gargalos de emissão, validação, envio ou recebimento.',
    calculoTecnico: 'COUNT(1)',
    calculoNegocio:
      'Conta cada fatura dentro do status de processo normalizado, expondo em qual etapa a carteira está concentrada.',
    agrupamento: 'GROUP BY status_processo',
  },

  contasPagarSerie: {
    ...contasPagarBase,
    descricao: 'Evolui valores pagos e valores ainda em aberto por mês de emissão para leitura de desembolso e obrigação financeira.',
    calculoTecnico: 'SUM([Valor pago]); SUM([Valor a pagar] - [Valor pago])',
    calculoNegocio:
      'Soma o que já foi pago e calcula o saldo aberto subtraindo o pago do valor a pagar, organizando a visão pelo mês de emissão.',
    agrupamento: 'GROUP BY CONVERT(CHAR(7), [Emissão], 23)',
  },
  contasPagarTopFornecedores: {
    ...contasPagarBase,
    descricao: 'Identifica fornecedores com maior valor a pagar e volume de títulos, apoiando negociação e priorização de caixa.',
    calculoTecnico: 'SUM([Valor a pagar]); COUNT(1)',
    calculoNegocio:
      'Soma o valor a pagar por fornecedor e conta quantos títulos compõem a obrigação, mantendo fornecedor vazio como "Sem fornecedor".',
    agrupamento: "GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Fornecedor/Nome]))), N''), N'Sem fornecedor')",
  },
  contasPagarCentroCusto: {
    ...contasPagarBase,
    descricao: 'Distribui contas a pagar por centro de custo para apontar onde as obrigações financeiras estão concentradas.',
    calculoTecnico: 'SUM([Valor a pagar])',
    calculoNegocio:
      'Soma o valor a pagar de cada centro de custo e preserva registros sem classificação em "Sem centro de custo".',
    agrupamento: "GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Centro de custo/Nome]))), N''), N'Sem centro de custo')",
  },
  contasPagarConciliacao: {
    ...contasPagarBase,
    descricao: 'Separa contas a pagar por situação de conciliação para evidenciar pendências de conferência financeira.',
    calculoTecnico: 'COUNT(1); SUM([Valor a pagar])',
    calculoNegocio:
      'Conta títulos e soma valor a pagar em cada situação de conciliação, usando "Sem conciliação" quando o campo vem vazio.',
    agrupamento: "GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Conciliado]))), N''), N'Sem conciliação')",
  },

  integracoesSazonalidade: {
    ...integracoesBase,
    descricao: 'Acompanha a evolução diária de sucessos e erros das integrações de XML e comprovantes com os destinos atendidos.',
    calculoTecnico: 'Leitura do endpoint /api/auditoria/integracoes-clientes/evolucao-diaria no Satélite.',
    calculoNegocio:
      'Para cada dia do período filtrado, exibe a quantidade de integrações bem-sucedidas e a quantidade de falhas retornadas pela auditoria operacional.',
    agrupamento: 'Agrupado por data de processamento no Satélite.',
  },
  integracoesSaudeDestino: {
    ...integracoesBase,
    descricao: 'Compara o volume de registros integrados e com erro por sistema destino para localizar concentração operacional.',
    calculoTecnico: 'Leitura das métricas consolidadas do endpoint /api/auditoria/integracoes-clientes no Satélite.',
    calculoNegocio:
      'Calcula sucessos por destino a partir do total de registros e do percentual de sucesso de XML; o restante é apresentado como erro.',
    agrupamento: 'Agrupado por sistema destino.',
  },

  cotacoesSerie: {
    ...cotacoesBase,
    descricao: 'Evolui cotações por data com conversões, perdas e valor de frete para medir geração e qualidade da demanda comercial.',
    calculoTecnico:
      "COUNT(1); SUM(CASE WHEN status_normalizado IN (N'convertida', N'convertido') THEN 1 ELSE 0 END); SUM(CASE WHEN status_normalizado IN (N'reprovada', N'reprovado', N'perdida', N'perdido') THEN 1 ELSE 0 END); SUM(valor_frete)",
    calculoNegocio:
      'Conta cotações deduplicadas do dia, separa convertidas e perdidas/reprovadas, e soma o valor de frete associado à oportunidade.',
    agrupamento: 'GROUP BY data_cotacao',
  },
  cotacoesFunil: {
    ...cotacoesBase,
    descricao: 'Mostra o funil de cotações por status e valor de frete para avaliar avanço comercial e exposição de receita potencial.',
    calculoTecnico: 'COUNT(1); CAST(COALESCE(SUM(valor_frete), 0) AS DECIMAL(19,2))',
    calculoNegocio:
      'Conta quantas cotações estão em cada status e soma o valor de frete, conectando estágio comercial ao tamanho financeiro do funil.',
    agrupamento: 'GROUP BY status_exibicao',
  },
  cotacoesTaxasConversao: {
    ...cotacoesBase,
    descricao: 'Calcula taxa de conversão por valor e por quantidade para comparar eficiência comercial no tempo e por tipo de operação.',
    calculoTecnico:
      "valor: SUM(CASE WHEN convertida THEN valor_frete ELSE 0 END) / NULLIF(SUM(valor_frete), 0); quantidade: SUM(CASE WHEN convertida THEN 1 ELSE 0 END) / NULLIF(COUNT(1), 0)",
    calculoNegocio:
      'Divide o valor convertido pelo valor total cotado e também divide a quantidade convertida pela quantidade total, evitando divisão por zero.',
    agrupamento: 'GROUP BY data_cotacao para evolução; GROUP BY tipo_operacao_normalizado para blocos LTL/FTL/PTL',
  },
  cotacoesTrechos: {
    ...cotacoesBase,
    descricao: 'Ranqueia trechos e UFs por valor cotado, valor convertido e volume para localizar rotas com maior oportunidade comercial.',
    calculoTecnico:
      'CAST(COALESCE(SUM(valor_frete), 0) AS DECIMAL(19,2)); SUM(CASE WHEN convertida THEN valor_frete ELSE 0 END); COUNT(1); SUM(convertidas); SUM(reprovadas)',
    calculoNegocio:
      'Para cada trecho ou UF, soma o valor cotado, soma o valor convertido, conta cotações e separa resultados convertidos e reprovados.',
    agrupamento: 'GROUP BY trecho ou GROUP BY uf_origem ou GROUP BY uf_destino',
  },
  cotacoesMotivosPerda: {
    ...cotacoesBase,
    descricao: 'Expõe motivos de perda por dimensão comercial para entender por que oportunidades não avançaram.',
    calculoTecnico: 'COUNT(1)',
    calculoNegocio:
      'Conta cotações perdidas ou reprovadas e as agrupa por motivo, cliente pagador ou trecho conforme o nível de detalhamento selecionado.',
    agrupamento: 'GROUP BY motivo_perda ou GROUP BY cliente_pagador ou GROUP BY trecho',
  },

  gestaoPerformanceRanking: {
    tabelasOrigem: 'dbo.fato_gestao_vista_fretes',
    cruzamentos: 'Nenhum JOIN; CTE performance no IndicadoresGestaoAVistaSqlRepository.',
    descricao: 'Ranqueia filiais pela performance de entrega no prazo para comparar qualidade operacional na gestão à vista.',
    calculoTecnico:
      'COUNT_BIG(1); SUM(CAST(is_no_prazo AS BIGINT)); SUM(CAST(is_fora_prazo AS BIGINT)); percentual Java = entregas_no_prazo / total_entregas',
    calculoNegocio:
      'Conta entregas por filial, soma quantas ficaram no prazo e fora do prazo, e calcula a taxa de pontualidade sobre o total avaliado.',
    agrupamento: 'GROUP BY previsao_entrega, filial_performance',
  },
  gestaoColetoresRanking: {
    tabelasOrigem: 'dbo.fato_gestao_vista_coletores',
    cruzamentos: 'Nenhum JOIN; CTE coletores_base -> coletores_agregados.',
    descricao: 'Compara filiais pela taxa de manifestos bipados para medir aderência do processo de coleta e conferência.',
    calculoTecnico:
      'ROUND((SUM(manifestos_bipados) * 100.0) / NULLIF(SUM(total_manifestos), 0), 1)',
    calculoNegocio:
      'Divide manifestos bipados pelo total de manifestos da filial e arredonda o percentual, evitando divisão por zero.',
    agrupamento: 'GROUP BY data_referencia, filial, classificacao',
  },
  gestaoCubagemRanking: {
    tabelasOrigem: 'dbo.fato_gestao_vista_fretes',
    cruzamentos: 'Nenhum JOIN; CTE cubagem no IndicadoresGestaoAVistaSqlRepository.',
    descricao: 'Ranqueia filiais pela proporção de fretes cubados para medir aderência à captura de cubagem.',
    calculoTecnico: 'COUNT_BIG(1); SUM(CAST(is_cubado AS BIGINT)); percentual Java = fretes_cubados / total_fretes',
    calculoNegocio:
      'Conta fretes por filial, soma os que possuem cubagem e divide esse volume pelo total de fretes avaliados.',
    agrupamento: 'GROUP BY data_frete_date, filial',
  },
  gestaoIndenizacaoRanking: {
    tabelasOrigem: 'dbo.vw_sinistros_powerbi, dbo.fato_fretes_faturamento',
    cruzamentos:
      'LEFT JOIN faturamento_mensal fm ON fm.mes_ref = sm.mes_ref AND fm.filial = sm.filial; LEFT JOIN faturamento_periodo fp ON fp.filial = sm.filial',
    descricao: 'Relaciona indenizações de sinistros com faturamento para medir impacto financeiro relativo por filial e período.',
    calculoTecnico: 'SUM(ABS(valor_a_pagar_cliente)) / NULLIF(faturamento_base, 0)',
    calculoNegocio:
      'Soma o valor absoluto das indenizações a pagar ao cliente e divide pelo faturamento base, mostrando o peso percentual do sinistro sobre a receita.',
    agrupamento: 'GROUP BY DATEFROMPARTS(YEAR(data_abertura), MONTH(data_abertura), 1), filial',
  },
  gestaoHorariosRanking: {
    tabelasOrigem: 'dbo.raster_viagens, dbo.raster_viagem_paradas',
    cruzamentos:
      'INNER JOIN viagens_base vb ON vb.cod_solicitacao = p.cod_solicitacao; LEFT JOIN paradas p ON p.cod_solicitacao = v.cod_solicitacao; LEFT JOIN hc_apoio apoio ON apoio.origem_sm = rc.origem_sm AND apoio.destino_sm = rc.destino_sm; LEFT JOIN filiais_raster filial ON filial.origem_sm = rc.origem_sm',
    descricao: 'Ranqueia filiais pela saída no horário para acompanhar disciplina de início de viagens e impacto no cumprimento operacional.',
    calculoTecnico:
      'COUNT_BIG(1); SUM(CASE WHEN saiu_no_horario = 1 THEN 1 ELSE 0 END); SUM(CASE WHEN saiu_no_horario = 0 THEN 1 ELSE 0 END)',
    calculoNegocio:
      'Conta viagens avaliadas, separa as que saíram no horário das que atrasaram e permite comparar aderência por filial.',
    agrupamento: 'GROUP BY data_corte, filial',
  },

  executivoTendenciaFinanceira: {
    tabelasOrigem:
      'dbo.fato_fretes_faturamento, dbo.fato_gestao_vista_faturas, vw_contas_a_pagar_powerbi, vw_coletas_powerbi',
    cruzamentos: 'Nenhum JOIN único; ExecutivoService agrega em memória séries já consolidadas por serviço e por YearMonth.',
    descricao: 'Consolida tendência executiva de receita, faturamento, contas a pagar e backlog de coletas para leitura mensal integrada.',
    calculoTecnico:
      'receitaOperacional += SUM(receita_bruta); valorFaturado += SUM(valor_operacional); saldoAPagar += SUM(aberto); backlogColetas += total - finalizadas - canceladas',
    calculoNegocio:
      'Une séries mensais já consolidadas das áreas financeira e operacional, somando receitas, faturas, saldo aberto e coletas ainda não resolvidas.',
    agrupamento: 'YearMonth.parse(ponto.date/month); fontes SQL agrupam por dia ou CONVERT(CHAR(7), mês, 23)',
  },
  executivoFaturamentoBacklog: {
    tabelasOrigem: 'dbo.fato_gestao_vista_faturas, vw_coletas_powerbi',
    cruzamentos: 'Nenhum JOIN único; ExecutivoService cruza as séries por YearMonth em Map<YearMonth, ExecutivoTrendAccumulator>.',
    descricao: 'Compara faturamento mensal com backlog de coletas para expor relação entre geração financeira e pendência operacional.',
    calculoTecnico: 'valorFaturado += SUM(valor_operacional); backlogColetas += total - finalizadas - canceladas',
    calculoNegocio:
      'Soma o valor faturado no mês e calcula backlog removendo coletas finalizadas e canceladas do total de coletas do mesmo período.',
    agrupamento: 'YearMonth.parse(ponto.date/month)',
  },

  etlTaxasDiarias: {
    tabelasOrigem: 'vw_bi_monitoramento',
    cruzamentos: 'Nenhum JOIN; leitura via DashboardExportDefinition.ETL_SAUDE.',
    descricao: 'Acompanha execuções diárias do ETL e taxa de sucesso para medir estabilidade da carga de dados.',
    calculoTecnico:
      "COUNT(1); SUM(CASE WHEN status_normalizado <> N'success' THEN 1 ELSE 0 END); taxa_sucesso no frontend = (execucoes - erros) / execucoes",
    calculoNegocio:
      'Conta execuções do dia, identifica quantas falharam e calcula a proporção de execuções bem-sucedidas sobre o total processado.',
    agrupamento: 'GROUP BY data_execucao',
  },
  etlVolumeDiario: {
    tabelasOrigem: 'vw_bi_monitoramento',
    cruzamentos: 'Nenhum JOIN; leitura via DashboardExportDefinition.ETL_SAUDE.',
    descricao: 'Monitora volume de registros processados e duração média das execuções para acompanhar saúde e capacidade do ETL.',
    calculoTecnico: 'SUM(total_registros); AVG(CAST(duracao_segundos AS FLOAT))',
    calculoNegocio:
      'Soma os registros movimentados no dia e calcula a duração média das execuções, permitindo detectar queda de volume ou lentidão.',
    agrupamento: 'GROUP BY data_execucao',
  },
} as const satisfies Record<string, ChartDefinition>;

export type ChartDictionaryKey = keyof typeof chartDictionary;
