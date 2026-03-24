export interface ExecutivoOverview {
  updatedAt: string;
  receitaOperacional: number;
  valorFaturado: number;
  saldoAReceber: number;
  saldoAPagar: number;
  backlogColetas: number;
  cargasPrevisaoVencida: number;
  ocupacaoMediaManifestos: number;
}

export interface ExecutivoTrendPoint {
  month: string;
  receitaOperacional: number;
  valorFaturado: number;
  saldoAReceber: number;
  saldoAPagar: number;
  backlogColetas: number;
}
