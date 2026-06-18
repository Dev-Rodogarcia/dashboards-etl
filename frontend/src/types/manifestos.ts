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
  receitaTotalTransportada: number;
  capacidadeKg: number;
  itensFinalizados: number | null;
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

export interface KPIsManifestos {
  totalManifestos: number;
  emTransito: number;
  pendentes: number;
  encerrados: number;
  kmTotal: number;
  custoTotal: number;
  custoPorKg: number;
  custoPorKm: number;
  receitaPorKg: number | null;
  receitaPorKm: number;
}

export interface GaugeMetric {
  global: number;
  distribuicao: number;
  transferencia: number;
  cargaFechada: number;
}

export interface ManifestosCustoDiario {
  data: string;
  custoReal: number;
}

export interface ManifestosCustosEvolucao {
  orcamentoAplicavel: boolean;
  orcamentoConfigurado: boolean;
  observacao: string | null;
  totalDiasUteis: number;
  diasUteisDecorridos: number;
  diasUteisRestantes: number;
  orcamentoCusto: number;
  custoReal: number;
  limiteDiarioBase: number;
  custoMedioDiarioReal: number;
  saldoOrcamentario: number;
  limiteDiarioDinamico: number;
  tendenciaCusto: number;
  consumoOrcamento: number;
  serieDiaria: ManifestosCustoDiario[];
}

export interface PerformanceVeiculosDados {
  updatedAt?: string | null;
  kpis: KPIsManifestos;
  remuneracao: GaugeMetric;
  aproveitamento: GaugeMetric;
  efetividade: GaugeMetric;
  statusSazonal: Array<{ data: string; encerrado: number; emTransito: number; pendente: number }>;
  custosContrato: Array<{ tipoContrato: string; custoTotal: number }>;
  tiposVeiculo: Array<{ tipo: string; quantidade: number }>;
  custosEvolucao: ManifestosCustosEvolucao;
}

export type ManifestosTempoNivel = 'dia' | 'mes' | 'ano';

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
  numeroManifesto?: string;
  classificacoes?: string[];
  tiposCarga?: string[];
  tiposContrato?: string[];
  tipoMotorista?: string[];
}

export interface ManifestosCostGoalConfig {
  branchId: string;
  contractType?: string;
  contractTypeKey?: string;
  ano: number;
  mes: number;
  costGoal: number;
  updatedAt: string | null;
  updatedByName: string | null;
  configurado: boolean;
  mensagem: string | null;
}

export interface ManifestosCostGoalPayload {
  branchId: string;
  contractType?: string;
  contractTypeKey?: string;
  ano: number;
  mes: number;
  costGoal: number;
}
