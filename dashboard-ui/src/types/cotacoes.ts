export interface CotacaoResumoRow {
  numeroCotacao: number;
  dataCotacao: string | null;
  filial: string | null;
  solicitante: string | null;
  clientePagador: string | null;
  cliente: string | null;
  trecho: string | null;
  pesoTaxado: number;
  valorNf: number;
  valorFrete: number;
  tabela: string | null;
  statusConversao: string | null;
  motivoPerda: string | null;
  cteDataEmissao: string | null;
  nfseDataEmissao: string | null;
}

export type CotacaoRow = CotacaoResumoRow;

export interface CotacoesOverview {
  updatedAt: string;
  totalCotacoes: number;
  valorPotencial: number;
  freteMedio: number;
  freteKgMedio: number;
  taxaConversaoCte: number;
  taxaConversaoNfse: number;
  taxaReprovacao: number;
  tempoMedioConversaoHoras: number;
}

export interface CotacoesTrendPoint {
  date: string;
  cotacoes: number;
  convertidas: number;
  reprovadas: number;
}

export interface CotacoesFunil {
  etapa: string;
  total: number;
}

export interface CotacoesCorredorValioso {
  trecho: string;
  valorFrete: number;
  cotacoes: number;
}

export interface CotacoesMotivoPerda {
  motivo: string;
  total: number;
}

export interface CotacoesCharts {
  funil: CotacoesFunil[];
  corredoresMaisValiosos: CotacoesCorredorValioso[];
  motivosPerda: CotacoesMotivoPerda[];
}

export interface CotacoesFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  clientes?: string[];
  ufOrigem?: string[];
  ufDestino?: string[];
  statusConversao?: string[];
  tabelas?: string[];
}
