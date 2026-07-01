import type {
  FreteResumoRow,
  FretesCharts,
  FretesClienteRanking,
  FretesDocumentMix,
  FretesFaturamentoDiario,
  FretesFiltro,
  FretesGoalConfig,
  FretesGoalConfigPayload,
  FretesGoalReplicarPayload,
  FretesGoalSummary,
  FretesOverview,
  FretesTrendPoint,
} from './fretes';

export type FaturamentoResumoRow = FreteResumoRow;
export type FaturamentoRow = FaturamentoResumoRow;
export type FaturamentoOverview = FretesOverview;
export type FaturamentoDiario = FretesFaturamentoDiario;
export type FaturamentoTrendPoint = FretesTrendPoint;
export type FaturamentoClienteRanking = FretesClienteRanking;
export type FaturamentoDocumentMix = FretesDocumentMix;
export type FaturamentoCharts = FretesCharts;
export type FaturamentoFiltro = FretesFiltro;
export type FaturamentoGoalConfig = FretesGoalConfig;
export type FaturamentoGoalConfigPayload = FretesGoalConfigPayload;
export type FaturamentoGoalReplicarPayload = FretesGoalReplicarPayload;
export type FaturamentoGoalSummary = FretesGoalSummary;
