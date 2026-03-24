export interface FaturaOperacionalRow {
  documento: string;
  emissao: string | null;
  filial: string | null;
  clientePagador: string | null;
  valorOperacional: number;
  statusProcesso: string;
}

export interface TituloReceberRow {
  id: number;
  documentoFinanceiro: string;
  emissao: string;
  vencimento: string | null;
  valor: number;
  valorPago: number;
  valorAPagar: number;
  pago: boolean;
  filialNome: string | null;
}

export interface FaturaReconciliacaoRow {
  documento: string;
  emissao: string | null;
  clientePagador: string | null;
  valorOperacional: number | null;
  valorFinanceiro: number | null;
  status: 'conciliado' | 'divergente' | 'sem-titulo';
}

export interface FaturaResumoRow {
  documento: string;
  emissao: string | null;
  vencimento: string | null;
  filial: string | null;
  clientePagador: string | null;
  valorOperacional: number;
  valorFinanceiro: number;
  valorPago: number;
  valorAberto: number;
  statusProcesso: string;
  statusFinanceiro: string;
}

export interface FaturasOverview {
  updatedAt: string;
  valorFaturado: number;
  valorRecebido: number;
  saldoAberto: number;
  taxaAdimplencia: number;
  dsoMedioDias: number;
  titulosEmAtraso: number;
  clientesAtivos: number;
  hasFinancialData: boolean;
}

export interface FaturasMensalTrend {
  month: string;
  faturado: number;
  pago: number;
  saldoAberto: number;
}

export interface FaturasAgingBucket {
  faixa: string;
  valor: number;
  titulos: number;
}

export interface FaturasClienteTop {
  cliente: string;
  faturado: number;
  saldoAberto: number;
}

export interface FaturasStatusProcesso {
  statusProcesso: string;
  total: number;
}

export interface FaturasFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  pagadores?: string[];
  statusProcesso?: string[];
  pago?: string[];
}
