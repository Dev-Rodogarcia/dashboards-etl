export interface ContaPagarResumoRow {
  lancamentoNumero: number;
  documentoNumero: string | null;
  emissao: string | null;
  tipo: string | null;
  filial: string | null;
  fornecedor: string | null;
  valor: number;
  valorPago: number;
  valorAPagar: number;
  classificacao: string | null;
  descricaoContabil: string | null;
  centroCusto: string | null;
  dataLiquidacao: string | null;
  statusPagamento: string | null;
  conciliado: string | null;
}

export type ContaPagarRow = ContaPagarResumoRow;

export interface ContasAPagarOverview {
  updatedAt: string;
  valorAPagar: number;
  valorPago: number;
  saldoAberto: number;
  taxaLiquidacao: number;
  leadTimeLiquidacaoDias: number;
  pctConciliado: number;
}

export interface ContasAPagarMensalTrend {
  month: string;
  pago: number;
  aberto: number;
}

export interface ContasAPagarPlanoConta {
  conta: string;
  valor: number;
  classificacao: string;
}

export interface ContasAPagarFornecedor {
  fornecedor: string;
  valor: number;
  titulos: number;
}

export interface ContasAPagarCentroCusto {
  centroCusto: string;
  valor: number;
}

export interface ContasAPagarConciliacao {
  status: string;
  total: number;
  valor: number;
}

export interface ContasAPagarCharts {
  topFornecedores: ContasAPagarFornecedor[];
  centroCusto: ContasAPagarCentroCusto[];
  conciliacao: ContasAPagarConciliacao[];
}

export interface ContasAPagarFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  fornecedores?: string[];
  classificacoes?: string[];
  centrosCusto?: string[];
  pago?: string[];
  conciliado?: string[];
}
