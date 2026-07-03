import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  CubagemMercadoriasOverview,
  CubagemMercadoriasRow,
  CubagemMercadoriasSeriePoint,
  HorarioCorteRow,
  HorariosCorteOverview,
  HorariosCorteSeriePoint,
  IndenizacaoMercadoriasOverview,
  IndenizacaoMercadoriasRow,
  IndenizacaoMercadoriasSeriePoint,
  IndicadoresGestaoVistaFiltro,
  KpiGoalEffectiveResponse,
  KpiGoalHistoryItem,
  KpiGoalIndicatorKey,
  KpiGoalOverridesResponse,
  KpiGoalsFullResponse,
  KpiGoalsUpdatePayload,
  PerformanceEntregaOverview,
  PerformanceEntregaRow,
  PerformanceEntregaSeriePoint,
  UtilizacaoColetoresOverview,
  UtilizacaoColetoresRankingItem,
  UtilizacaoColetoresRow,
  UtilizacaoColetoresSeriePoint,
  ViagemJustificativa,
  ViagemJustificativaPayload,
} from '../../types/indicadoresGestaoAVista';
import { normalizarCompetenciaApiOpcional } from '../../utils/competencia';

const BASE = '/api/painel/indicadores-gestao-a-vista';
const KPI_GOALS_BASE = '/api/kpi-goals';

export const GLOBAL_KPI_GOAL_BRANCH_ID = 'GLOBAL';

function withCompetenciaParam<TParams extends Record<string, string | number>>(
  params: TParams,
  competencia?: string,
): TParams | (TParams & { competencia: string }) {
  const competenciaApi = normalizarCompetenciaApiOpcional(competencia);
  return competenciaApi ? { ...params, competencia: competenciaApi } : params;
}

function normalizePayloadCompetencia(payload: KpiGoalsUpdatePayload): KpiGoalsUpdatePayload {
  const competencia = normalizarCompetenciaApiOpcional(payload.competencia);
  return competencia ? { ...payload, competencia } : payload;
}

function withLimit(filtro: IndicadoresGestaoVistaFiltro, limite: number) {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  return params;
}

export async function buscarPerformanceEntregaOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<PerformanceEntregaOverview> {
  const { data } = await clienteAxios.get<PerformanceEntregaOverview>(`${BASE}/performance-entrega/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarPerformanceEntregaSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<PerformanceEntregaSeriePoint[]> {
  const { data } = await clienteAxios.get<PerformanceEntregaSeriePoint[]>(`${BASE}/performance-entrega/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarPerformanceEntregaTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<PerformanceEntregaRow[]> {
  const { data } = await clienteAxios.get<PerformanceEntregaRow[]>(`${BASE}/performance-entrega/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarPerformanceEntregaTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<PerformanceEntregaRow>> {
  return buscarTabelaPaginada(`${BASE}/performance-entrega/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarPerformanceEntregaCsv(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarCsv(`${BASE}/performance-entrega/exportacao`, filtro, 'indicadores-performance-entrega');
}

export async function buscarUtilizacaoColetoresOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<UtilizacaoColetoresOverview> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresOverview>(`${BASE}/utilizacao-coletores/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarUtilizacaoColetoresSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<UtilizacaoColetoresSeriePoint[]> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresSeriePoint[]>(`${BASE}/utilizacao-coletores/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarUtilizacaoColetoresRanking(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<UtilizacaoColetoresRankingItem[]> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresRankingItem[]>(`${BASE}/utilizacao-coletores/ranking`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarUtilizacaoColetoresTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<UtilizacaoColetoresRow[]> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresRow[]>(`${BASE}/utilizacao-coletores/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarUtilizacaoColetoresTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<UtilizacaoColetoresRow>> {
  return buscarTabelaPaginada(`${BASE}/utilizacao-coletores/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarUtilizacaoColetoresCsv(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarCsv(`${BASE}/utilizacao-coletores/exportacao`, filtro, 'indicadores-utilizacao-coletores');
}

export async function buscarCubagemMercadoriasOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<CubagemMercadoriasOverview> {
  const { data } = await clienteAxios.get<CubagemMercadoriasOverview>(`${BASE}/cubagem-mercadorias/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCubagemMercadoriasSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<CubagemMercadoriasSeriePoint[]> {
  const { data } = await clienteAxios.get<CubagemMercadoriasSeriePoint[]>(`${BASE}/cubagem-mercadorias/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCubagemMercadoriasTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<CubagemMercadoriasRow[]> {
  const { data } = await clienteAxios.get<CubagemMercadoriasRow[]>(`${BASE}/cubagem-mercadorias/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarCubagemMercadoriasTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<CubagemMercadoriasRow>> {
  return buscarTabelaPaginada(`${BASE}/cubagem-mercadorias/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarCubagemMercadoriasCsv(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarCsv(`${BASE}/cubagem-mercadorias/exportacao`, filtro, 'indicadores-cubagem-mercadorias');
}

export async function buscarIndenizacaoMercadoriasOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<IndenizacaoMercadoriasOverview> {
  const { data } = await clienteAxios.get<IndenizacaoMercadoriasOverview>(`${BASE}/indenizacao-mercadorias/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarIndenizacaoMercadoriasSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<IndenizacaoMercadoriasSeriePoint[]> {
  const { data } = await clienteAxios.get<IndenizacaoMercadoriasSeriePoint[]>(`${BASE}/indenizacao-mercadorias/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarIndenizacaoMercadoriasTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<IndenizacaoMercadoriasRow[]> {
  const { data } = await clienteAxios.get<IndenizacaoMercadoriasRow[]>(`${BASE}/indenizacao-mercadorias/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarIndenizacaoMercadoriasTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<IndenizacaoMercadoriasRow>> {
  return buscarTabelaPaginada(`${BASE}/indenizacao-mercadorias/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarIndenizacaoMercadoriasCsv(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarCsv(`${BASE}/indenizacao-mercadorias/exportacao`, filtro, 'indicadores-indenizacao-mercadorias');
}

export async function buscarHorariosCorteOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<HorariosCorteOverview> {
  const { data } = await clienteAxios.get<HorariosCorteOverview>(`${BASE}/horarios-corte/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarHorariosCorteSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<HorariosCorteSeriePoint[]> {
  const { data } = await clienteAxios.get<HorariosCorteSeriePoint[]>(`${BASE}/horarios-corte/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarHorariosCorteTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<HorarioCorteRow[]> {
  const { data } = await clienteAxios.get<HorarioCorteRow[]>(`${BASE}/horarios-corte/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarHorariosCorteTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<HorarioCorteRow>> {
  return buscarTabelaPaginada(`${BASE}/horarios-corte/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarHorariosCorteCsv(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarCsv(`${BASE}/horarios-corte/exportacao`, filtro, 'indicadores-horarios-corte');
}

export async function salvarJustificativaHorarioCorte(
  payload: ViagemJustificativaPayload,
): Promise<ViagemJustificativa> {
  const { data } = await clienteAxios.post<ViagemJustificativa>(`${BASE}/horarios-corte/justificativas`, payload);
  return data;
}

export async function excluirJustificativaHorarioCorte(sm: number | string): Promise<void> {
  await clienteAxios.delete(`${BASE}/horarios-corte/justificativas/${encodeURIComponent(String(sm))}`);
}

export async function buscarKpiGoalsCompleto(competencia?: string): Promise<KpiGoalsFullResponse> {
  const params = withCompetenciaParam({}, competencia);
  const { data } = Object.keys(params).length > 0
    ? await clienteAxios.get<KpiGoalsFullResponse>(KPI_GOALS_BASE, { params })
    : await clienteAxios.get<KpiGoalsFullResponse>(KPI_GOALS_BASE);
  return data;
}

export async function buscarKpiGoalsEfetivos(branchId: string, competencia?: string): Promise<KpiGoalEffectiveResponse> {
  const { data } = await clienteAxios.get<KpiGoalEffectiveResponse>(`${KPI_GOALS_BASE}/effective`, {
    params: withCompetenciaParam({ branchId }, competencia),
  });
  return data;
}

export async function atualizarKpiGoalsGlobais(payload: KpiGoalsUpdatePayload): Promise<KpiGoalsFullResponse> {
  const payloadNormalizado = normalizePayloadCompetencia(payload);
  const competencia = normalizarCompetenciaApiOpcional(payloadNormalizado.competencia);
  const { data } = competencia
    ? await clienteAxios.put<KpiGoalsFullResponse>(`${KPI_GOALS_BASE}/global`, payloadNormalizado, { params: { competencia } })
    : await clienteAxios.put<KpiGoalsFullResponse>(`${KPI_GOALS_BASE}/global`, payloadNormalizado);
  return data;
}

export async function atualizarKpiGoalsFilial(branchId: string, payload: KpiGoalsUpdatePayload): Promise<KpiGoalEffectiveResponse> {
  const payloadNormalizado = normalizePayloadCompetencia(payload);
  const competencia = normalizarCompetenciaApiOpcional(payloadNormalizado.competencia);
  const { data } = competencia
    ? await clienteAxios.put<KpiGoalEffectiveResponse>(`${KPI_GOALS_BASE}/branch/${encodeURIComponent(branchId)}`, payloadNormalizado, { params: { competencia } })
    : await clienteAxios.put<KpiGoalEffectiveResponse>(`${KPI_GOALS_BASE}/branch/${encodeURIComponent(branchId)}`, payloadNormalizado);
  return data;
}

export async function removerKpiGoalsOverride(branchId: string, competencia?: string): Promise<KpiGoalEffectiveResponse> {
  const params = withCompetenciaParam({}, competencia);
  const { data } = Object.keys(params).length > 0
    ? await clienteAxios.delete<KpiGoalEffectiveResponse>(`${KPI_GOALS_BASE}/branch/${encodeURIComponent(branchId)}`, { params })
    : await clienteAxios.delete<KpiGoalEffectiveResponse>(`${KPI_GOALS_BASE}/branch/${encodeURIComponent(branchId)}`);
  return data;
}

export async function buscarKpiGoalsHistorico(branchId: string, limit = 10): Promise<KpiGoalHistoryItem[]> {
  const { data } = await clienteAxios.get<KpiGoalHistoryItem[]>(`${KPI_GOALS_BASE}/history`, {
    params: { branchId, limit },
  });
  return data;
}

export async function buscarKpiGoalsHistoricoPaginado(
  branchId: string,
  pagina = 1,
  tamanhoPagina = 10,
): Promise<PaginacaoResponse<KpiGoalHistoryItem>> {
  const { data } = await clienteAxios.get<PaginacaoResponse<KpiGoalHistoryItem>>(`${KPI_GOALS_BASE}/history/page`, {
    params: { branchId, pagina, tamanhoPagina },
  });
  return data;
}

export async function buscarKpiGoalOverrides(indicatorKey: KpiGoalIndicatorKey, competencia?: string): Promise<KpiGoalOverridesResponse> {
  const { data } = await clienteAxios.get<KpiGoalOverridesResponse>(`${KPI_GOALS_BASE}/overrides`, {
    params: withCompetenciaParam({ indicatorKey }, competencia),
  });
  return data;
}
