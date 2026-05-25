import type { GoalTone } from '../utils/indicadoresGestaoVistaUi';

export type PerformanceStatusEntrega =
  | 'Pendente'
  | 'Em Trânsito'
  | 'Finalizada'
  | 'Cancelada'
  | 'Em Tratativa';

export type PerformanceStatusPrazo = 'NO PRAZO' | 'FORA DO PRAZO';

export type PerformanceStatusDias =
  | 'NO PRAZO'
  | '1 DIA DE ATRASO'
  | '2 DIAS DE ATRASO'
  | '3 DIAS DE ATRASO'
  | 'ACIMA DE 3 DIAS DE ATRASO'
  | '1 DIA ANTES'
  | '2 DIAS ANTES'
  | '3 DIAS ANTES'
  | 'ACIMA DE 3 DIAS ANTES';

export interface Entrega {
  numeroMinuta: number;
  status: PerformanceStatusEntrega;
  dataPrevisaoEntrega: string;
  dataFinalizacao: string | null;
  responsavelRegiaoDestino: string;
  filialEmissora: string;
  regiaoDestino: string;
  cidadeDestino: string;
  pesoTaxado: number;
  valorNotaFiscal: number;
  comprovanteAnexado: boolean;
}

export interface PerformanceEntregaRow extends Entrega {
  performanceStatus: PerformanceStatusPrazo | null;
  performanceStatusDias: PerformanceStatusDias | null;
}

export interface PerformanceTabelaRow extends PerformanceEntregaRow {
  performanceDiferencaDias: number | null;
}

export interface PerformanceTabelaPage {
  content: PerformanceTabelaRow[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PerformanceFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
  status?: string[];
  pagadores?: string[];
  responsaveis?: string[];
  regioesDestino?: string[];
  cidadesDestino?: string[];
}

export type PerformanceTempoNivel = 'dia' | 'mes' | 'ano';

export interface PerformanceOverview {
  updatedAt: string | null;
  totalEntregas: number;
  finalizadas: number;
  noPrazo: number;
  foraDoPrazo: number;
  performancePercentual: number;
  emAtraso: number;
  pesoTaxadoToneladas: number;
  comprovanteAnexadoPercentual: number;
  valorNfSemComprovante: number;
}

export interface PerformanceSerieTemporalPoint {
  date: string;
  total: number;
  finalizadas: number;
  emTransito: number;
  pendentes: number;
  canceladas: number;
  emTratativa: number;
}

export interface PerformanceStatusDistribuicao {
  status: PerformanceStatusEntrega;
  total: number;
}

export interface PerformanceHistoricoPoint {
  date: string;
  performancePercentual: number;
  metaPercentual: number;
  finalizadas: number;
  noPrazo: number;
}

export type PerformanceDrilldownNivel = 'responsavel' | 'regiao' | 'cidade';

export interface PerformanceDrilldownParams {
  nivel: PerformanceDrilldownNivel;
  responsavel?: string | null;
  regiaoDestino?: string | null;
}

export interface PerformanceDrilldownPoint {
  nome: string;
  nivel: PerformanceDrilldownNivel;
  noPrazo: number;
  foraDoPrazo: number;
  emAtraso: number;
  total: number;
}

export interface PerformanceAgingPoint {
  bucket: string;
  total: number;
}

export interface PerformanceKpiItem {
  label: string;
  valor: string;
  helperText?: string;
  tone?: GoalTone;
}
