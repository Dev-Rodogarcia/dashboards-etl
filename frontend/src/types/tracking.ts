export interface TrackingRawRow {
  numeroMinuta: number;
  dataFrete: string | null;
  tipo: string | null;
  volumes: number | null;
  pesoTaxadoRaw: string | null;
  valorNfRaw: string | null;
  valorFrete: number;
  filialEmissora: string | null;
  filialOrigem: string | null;
  filialAtual: string | null;
  filialDestino: string | null;
  regiaoOrigem: string | null;
  regiaoDestino: string | null;
  classificacao: string | null;
  statusCarga: string | null;
  previsaoEntrega: string | null;
}

export interface TrackingRow extends Omit<TrackingRawRow, 'pesoTaxadoRaw' | 'valorNfRaw'> {
  pesoTaxado: number | null;
  valorNf: number | null;
}

export interface TrackingOverview {
  updatedAt: string;
  totalCargas: number;
  emTransito: number;
  previsaoVencida: number;
  valorFreteEmCarteira: number;
  pesoTaxadoTotal: number;
  pctFinalizado: number;
}

export interface TrackingStatusDistribuicao {
  status: string;
  total: number;
  valorFrete: number;
}

export interface TrackingPrevisaoVencidaFilial {
  filialAtual: string;
  vencidas: number;
  total: number;
}

export interface TrackingOrigemDestino {
  origem: string;
  destino: string;
  cargas: number;
  valorFrete: number;
}

export interface TrackingValorPorRegiao {
  regiaoDestino: string;
  valorFrete: number;
  cargas: number;
}

export interface TrackingCharts {
  statusDistribuicao: TrackingStatusDistribuicao[];
  previsaoVencidaPorFilialAtual: TrackingPrevisaoVencidaFilial[];
  valorPorRegiaoDestino: TrackingValorPorRegiao[];
}

export interface TrackingTimelinePoint {
  date: string;
  pendente: number;
  emEntrega: number;
  emTransferencia: number;
  finalizado: number;
}

export interface TrackingFiltro {
  dataInicio: string;
  dataFim: string;
  filialEmissora?: string[];
  filialAtual?: string[];
  filialDestino?: string[];
  regiaoOrigem?: string[];
  regiaoDestino?: string[];
  statusCarga?: string[];
}
