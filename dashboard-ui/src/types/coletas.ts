export interface ColetaResumoRow {
  id: string;
  coleta: number | null;
  solicitacao: string | null;
  agendamento: string | null;
  finalizacao: string | null;
  status: string;
  volumes: number | null;
  pesoTaxado: number;
  valorNf: number;
  numeroManifesto: number | null;
  cliente: string | null;
  cidade: string | null;
  uf: string | null;
  regiaoColeta: string | null;
  filial: string | null;
  usuario: string | null;
  motivoCancelamento: string | null;
  numeroTentativas: number | null;
}

export type ColetaRow = ColetaResumoRow;

export interface ColetasOverview {
  updatedAt: string;
  totalColetas: number;
  finalizadas: number;
  taxaSucesso: number;
  taxaCancelamento: number;
  slaNoAgendamento: number;
  leadTimeMedioDias: number;
  tentativasMedias: number;
  pesoTaxadoTotal: number;
  valorNfTotal: number;
}

export interface ColetasTrendPoint {
  date: string;
  total: number;
  finalizadas: number;
  canceladas: number;
  emTratativa: number;
}

export interface ColetasStatusDistribuicao {
  status: string;
  total: number;
}

export interface ColetasSlaPorFilial {
  filial: string;
  slaPct: number;
  total: number;
}

export interface ColetasRegiaoVolume {
  regiao: string;
  totalColetas: number;
  pesoTaxado: number;
  volumes: number;
}

export interface ColetasAgingBucket {
  faixa: string;
  total: number;
}

export interface ColetasCharts {
  statusDistribuicao: ColetasStatusDistribuicao[];
  slaPorFilial: ColetasSlaPorFilial[];
  regiaoVolume: ColetasRegiaoVolume[];
  agingAbertas: ColetasAgingBucket[];
}

export interface ColetasFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  clientes?: string[];
  status?: string[];
  regioes?: string[];
  usuarios?: string[];
}
