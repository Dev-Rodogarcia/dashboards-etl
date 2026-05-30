export interface CotacaoResumoRow {
  numeroCotacao: number;
  dataCotacao: string | null;
  filial: string | null;
  solicitante: string | null;
  clientePagador: string | null;
  cliente: string | null;
  trecho: string | null;
  valorFrete: number;
  statusConversao: string | null;
  motivoPerda: string | null;
  tipoOperacao: string | null;
  volumes: number | null;
  pesoTaxado: number;
  fretePorKg: number;
  minFreteKg: number;
  valorNf: number;
  percentualNf: number;
  tabela: string | null;
  origem: string | null;
  destino: string | null;
  cteDataEmissao: string | null;
  nfseDataEmissao: string | null;
}

export type CotacaoRow = CotacaoResumoRow;

export interface CotacoesOverview {
  updatedAt: string;
  totalCotacoes: number;
  valorPotencial: number;
  valorConvertido: number;
  freteMedio: number;
  freteKgMedio: number;
  conversaoValor: number;
  conversaoQuantidade: number;
  reprovacaoPercentual: number;
  taxaConversaoCte: number;
  taxaConversaoNfse: number;
  tempoMedioConversaoHoras: number;
}

export interface CotacoesTrendPoint {
  date: string;
  cotacoes: number;
  convertidas: number;
  reprovadas: number;
  valorPotencial: number;
  valorConvertido: number;
}

export interface CotacoesFunil {
  etapa: string;
  total: number;
  valor: number;
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

export interface CotacoesAgrupamento {
  nome: string;
  valorPotencial: number;
  valorConvertido: number;
  cotacoes: number;
  convertidas: number;
  reprovadas: number;
}

export interface CotacoesCharts {
  funil: CotacoesFunil[];
  corredoresMaisValiosos: CotacoesCorredorValioso[];
  motivosPerda: CotacoesMotivoPerda[];
  trechosMaisValiosos: CotacoesAgrupamento[];
  trechosPorUfOrigem: CotacoesAgrupamento[];
  trechosPorUfDestino: CotacoesAgrupamento[];
  conversaoPorTipoOperacao: CotacoesAgrupamento[];
  perdasPorCliente: CotacoesMotivoPerda[];
  perdasPorTrecho: CotacoesMotivoPerda[];
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
  usuarios?: string[];
}
