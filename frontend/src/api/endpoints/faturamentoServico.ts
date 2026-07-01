import type { PaginacaoResponse } from '../../types/common';
import type {
  FaturamentoCharts,
  FaturamentoClienteRanking,
  FaturamentoDocumentMix,
  FaturamentoFiltro,
  FaturamentoGoalConfig,
  FaturamentoGoalConfigPayload,
  FaturamentoGoalReplicarPayload,
  FaturamentoGoalSummary,
  FaturamentoOverview,
  FaturamentoResumoRow,
  FaturamentoTrendPoint,
} from '../../types/faturamento';
import type { TableApiFilters } from '../../types/tableFilters';
import {
  buscarFretesGraficos,
  buscarFretesMetas,
  buscarFretesMetasConfiguracoes,
  buscarFretesMixDocumental,
  buscarFretesOverview,
  buscarFretesSerie,
  buscarFretesTabela,
  buscarFretesTabelaPaginada,
  buscarFretesTabelaTotal,
  buscarFretesTopClientes,
  exportarFretesCsv,
  replicarFretesMetasConfiguracoes,
  removerFretesMetaConfiguracao,
  salvarFretesMetaConfiguracao,
} from './fretesServico';

export const FATURAMENTO_LEGACY_API_BASE = '/api/painel/fretes';

export function mapFretesOverviewToFaturamento(data: FaturamentoOverview): FaturamentoOverview {
  return { ...data };
}

export async function buscarFaturamentoOverview(filtro: FaturamentoFiltro): Promise<FaturamentoOverview> {
  return mapFretesOverviewToFaturamento(await buscarFretesOverview(filtro));
}

export async function buscarFaturamentoSerie(filtro: FaturamentoFiltro): Promise<FaturamentoTrendPoint[]> {
  return buscarFretesSerie(filtro);
}

export async function buscarFaturamentoTopClientes(
  filtro: FaturamentoFiltro,
  limite = 10,
): Promise<FaturamentoClienteRanking[]> {
  return buscarFretesTopClientes(filtro, limite);
}

export async function buscarFaturamentoMixDocumental(filtro: FaturamentoFiltro): Promise<FaturamentoDocumentMix[]> {
  return buscarFretesMixDocumental(filtro);
}

export async function buscarFaturamentoGraficos(filtro: FaturamentoFiltro): Promise<FaturamentoCharts> {
  return buscarFretesGraficos(filtro);
}

export async function buscarFaturamentoMetas(filtro: FaturamentoFiltro): Promise<FaturamentoGoalSummary> {
  return buscarFretesMetas(filtro);
}

export async function buscarFaturamentoMetasConfiguracoes(ano: number, mes: number): Promise<FaturamentoGoalConfig[]> {
  return buscarFretesMetasConfiguracoes(ano, mes);
}

export async function salvarFaturamentoMetaConfiguracao(payload: FaturamentoGoalConfigPayload): Promise<FaturamentoGoalConfig> {
  return salvarFretesMetaConfiguracao(payload);
}

export async function replicarFaturamentoMetasConfiguracoes(
  payload: FaturamentoGoalReplicarPayload,
): Promise<FaturamentoGoalConfig[]> {
  return replicarFretesMetasConfiguracoes(payload);
}

export async function removerFaturamentoMetaConfiguracao(branchId: string, ano: number, mes: number): Promise<void> {
  await removerFretesMetaConfiguracao(branchId, ano, mes);
}

export async function buscarFaturamentoTabela(
  filtro: FaturamentoFiltro,
  limite = 100,
): Promise<FaturamentoResumoRow[]> {
  return buscarFretesTabela(filtro, limite);
}

export async function buscarFaturamentoTabelaTotal(filtro: FaturamentoFiltro): Promise<number> {
  return buscarFretesTabelaTotal(filtro);
}

export async function buscarFaturamentoTabelaPaginada(
  filtro: FaturamentoFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<FaturamentoResumoRow>> {
  return buscarFretesTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela);
}

export async function exportarFaturamentoCsv(filtro: FaturamentoFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await exportarFretesCsv(filtro, filtrosTabela);
}
