export interface ManifestoResumoRow {
  numero: number;
  identificadorUnico: string | null;
  status: string;
  classificacao: string | null;
  filial: string | null;
  dataCriacao: string | null;
  fechamento: string | null;
  motorista: string | null;
  veiculoPlaca: string | null;
  tipoVeiculo: string | null;
  totalPesoTaxado: number;
  totalM3: number;
  custoTotal: number;
  valorFrete: number;
  combustivel: number;
  pedagio: number;
  saldoPagar: number;
  kmTotal: number;
  itensTotal: number | null;
}

export type ManifestoRow = ManifestoResumoRow;

export interface ManifestosOverview {
  updatedAt: string;
  totalManifestos: number;
  emTransito: number;
  encerrados: number;
  kmTotal: number;
  custoTotal: number;
  custoPorKm: number;
  ocupacaoPesoMediaPct: number;
  ocupacaoCubagemMediaPct: number;
}

export interface ManifestosTrendPoint {
  date: string;
  encerrado: number;
  emTransito: number;
  pendente: number;
}

export interface ManifestosCustoPorFilial {
  filial: string;
  custoTotal: number;
  km: number;
  custoPorKm: number;
}

export interface ManifestosOcupacaoScatter {
  pesoTaxado: number;
  totalM3: number;
  custoTotal: number;
}

export interface ManifestosRankingMotorista {
  motorista: string;
  manifestos: number;
  km: number;
  custoTotal: number;
}

export interface ManifestosComposicaoCusto {
  categoria: string;
  valor: number;
}

export interface ManifestosCharts {
  custoPorFilial: ManifestosCustoPorFilial[];
  rankingMotorista: ManifestosRankingMotorista[];
  composicaoCusto: ManifestosComposicaoCusto[];
  ocupacaoScatter: ManifestosOcupacaoScatter[];
}

export interface ManifestosFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  status?: string[];
  motoristas?: string[];
  veiculos?: string[];
  tiposCarga?: string[];
  tiposContrato?: string[];
}
