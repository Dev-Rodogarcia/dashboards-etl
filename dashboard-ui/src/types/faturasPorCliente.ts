export interface FaturasPorClienteOverview {
  updatedAt: string;
  valorFaturado: number;
  registrosFaturados: number;
  aguardandoFaturamento: number;
  titulosEmAtraso: number;
  prazoMedioDias: number;
  clientesAtivos: number;
}

export interface FaturasPorClienteMensalTrend {
  month: string;
  valorFaturado: number;
  registrosFaturados: number;
}

export interface FaturasPorClienteAgingBucket {
  faixa: string;
  valor: number;
  titulos: number;
}

export interface FaturasPorClienteTopCliente {
  cliente: string;
  valorFaturado: number;
}

export interface FaturasPorClienteStatusProcesso {
  statusProcesso: string;
  total: number;
}

export interface FaturaPorClienteResumoRow {
  idUnico: string;
  documentoFatura: string | null;
  emissao: string | null;
  vencimento: string | null;
  baixa: string | null;
  filial: string | null;
  clientePagador: string | null;
  numeroCte: number | null;
  valorFaturado: number;
  statusProcesso: string;
}

export interface FaturasPorClienteFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  pagadores?: string[];
  statusProcesso?: string[];
}
