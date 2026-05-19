export interface FreteResumoRow {
  id: number;
  numeroMinuta: number | null;
  dataFrete: string | null;
  status: string;
  filial: string | null;
  pagador: string | null;
  remetente: string | null;
  destinatario: string | null;
  origemUf: string | null;
  destinoUf: string | null;
  valorTotalServico: number;
  valorFrete: number;
  pesoTaxado: number;
  volumes: number | null;
  previsaoEntrega: string | null;
  documentoTipo: string;
  numeroCte: number | null;
  numeroNfse: number | null;
  valorIcms: number;
  valorPis: number;
  valorCofins: number;
  metaFaturamento?: number;
  percentualAtingimentoFaturamento?: number;
  metaFretes?: number;
  percentualAtingimentoFretes?: number;
}

export type FreteRow = FreteResumoRow;

export interface FretesOverview {
  updatedAt: string;
  totalFretes: number;
  receitaBruta: number;
  valorFrete: number;
  ticketMedio: number;
  pesoTaxadoTotal: number;
  volumesTotais: number;
  pctCteEmitido: number;
  pctNfseEmitida: number;
  fretesPrevisaoVencida: number;
  metaFaturamento: number;
  percentualAtingimentoFaturamento: number;
  metaFretes: number;
  percentualAtingimentoFretes: number;
}

export interface FretesTrendPoint {
  date: string;
  receitaBruta: number;
  valorFrete: number;
  fretes: number;
}

export interface FretesClienteRanking {
  cliente: string;
  receita: number;
  fretes: number;
  ticketMedio: number;
}

export interface FretesPrevisaoVencida {
  status: string;
  vencidos: number;
  noPrazo: number;
}

export interface FretesDocumentMix {
  tipoDocumento: string;
  total: number;
}

export interface FretesOrigemDestino {
  origemUf: string;
  destinoUf: string;
  receita: number;
  fretes: number;
}

export interface FretesFaturamentoGrupo {
  nome: string;
  receita: number;
  fretes: number;
}

export interface FretesCharts {
  previsaoPorStatus: FretesPrevisaoVencida[];
  topRotasPorReceita: FretesOrigemDestino[];
  faturamentoPorClassificacao: FretesFaturamentoGrupo[];
  faturamentoPorResponsavelDestino: FretesFaturamentoGrupo[];
  faturamentoPorUfOrigem: FretesFaturamentoGrupo[];
  faturamentoPorUfDestino: FretesFaturamentoGrupo[];
  faturamentoPorCidadeDestino: FretesFaturamentoGrupo[];
}

export interface FretesFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  status?: string[];
  pagadores?: string[];
  ufOrigem?: string[];
  ufDestino?: string[];
  tiposFrete?: string[];
  modais?: string[];
}

export interface FretesGoalBranchSummary {
  branchId: string;
  metaFaturamento: number;
  realizadoFaturamento: number;
  percentualAtingimentoFaturamento: number;
  metaFretes: number;
  realizadoFretes: number;
  percentualAtingimentoFretes: number;
}

export interface FretesGoalSummary {
  dataInicio: string;
  dataFim: string;
  metaFaturamento: number;
  realizadoFaturamento: number;
  percentualAtingimentoFaturamento: number;
  metaFretes: number;
  realizadoFretes: number;
  percentualAtingimentoFretes: number;
  branches: FretesGoalBranchSummary[];
}

export interface FretesGoalConfig {
  branchId: string;
  ano: number;
  mes: number;
  metaFaturamento: number;
  metaFretes: number;
  updatedAt: string | null;
  updatedByName: string | null;
  configurado: boolean;
  mensagem: string | null;
}

export interface FretesGoalConfigPayload {
  branchId: string;
  ano: number;
  mes: number;
  metaFaturamento: number;
  metaFretes: number;
}
