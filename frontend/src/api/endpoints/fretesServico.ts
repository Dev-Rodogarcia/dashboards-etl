import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  FretesCharts,
  FretesClienteRanking,
  FretesDocumentMix,
  FretesFiltro,
  FretesGoalConfig,
  FretesGoalConfigPayload,
  FretesGoalReplicarPayload,
  FretesGoalSummary,
  FretesOverview,
  FretesTrendPoint,
  FreteResumoRow,
} from '../../types/fretes';
import type { TableApiFilters } from '../../types/tableFilters';

export async function buscarFretesOverview(filtro: FretesFiltro): Promise<FretesOverview> {
  const { data } = await clienteAxios.get<FretesOverview>('/api/painel/fretes', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFretesSerie(filtro: FretesFiltro): Promise<FretesTrendPoint[]> {
  const { data } = await clienteAxios.get<FretesTrendPoint[]>('/api/painel/fretes/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFretesTopClientes(
  filtro: FretesFiltro,
  limite = 10
): Promise<FretesClienteRanking[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FretesClienteRanking[]>('/api/painel/fretes/top-clientes', { params });
  return data;
}

export async function buscarFretesMixDocumental(filtro: FretesFiltro): Promise<FretesDocumentMix[]> {
  const { data } = await clienteAxios.get<FretesDocumentMix[]>('/api/painel/fretes/mix-documental', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFretesGraficos(filtro: FretesFiltro): Promise<FretesCharts> {
  const { data } = await clienteAxios.get<FretesCharts>('/api/painel/fretes/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFretesMetas(filtro: FretesFiltro): Promise<FretesGoalSummary> {
  const { data } = await clienteAxios.get<FretesGoalSummary>('/api/painel/fretes/metas', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFretesMetasConfiguracoes(ano: number, mes: number): Promise<FretesGoalConfig[]> {
  const { data } = await clienteAxios.get<FretesGoalConfig[]>('/api/painel/fretes/metas/configuracoes', {
    params: { ano, mes },
  });
  return data;
}

export async function salvarFretesMetaConfiguracao(payload: FretesGoalConfigPayload): Promise<FretesGoalConfig> {
  const { data } = await clienteAxios.put<FretesGoalConfig>('/api/painel/fretes/metas/configuracoes', payload);
  return data;
}

export async function replicarFretesMetasConfiguracoes(
  payload: FretesGoalReplicarPayload,
): Promise<FretesGoalConfig[]> {
  const { data } = await clienteAxios.post<FretesGoalConfig[]>('/api/painel/fretes/metas/replicar', payload);
  return data;
}

export async function removerFretesMetaConfiguracao(branchId: string, ano: number, mes: number): Promise<void> {
  await clienteAxios.delete('/api/painel/fretes/metas/configuracoes', {
    params: { branchId, ano, mes },
  });
}

export async function buscarFretesTabela(
  filtro: FretesFiltro,
  limite = 100
): Promise<FreteResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FreteResumoRow[]>('/api/painel/fretes/tabela', { params });
  return data;
}

export async function buscarFretesTabelaTotal(filtro: FretesFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/fretes/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarFretesTabelaPaginada(
  filtro: FretesFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<FreteResumoRow>> {
  return buscarTabelaPaginada('/api/painel/fretes/tabela/paginada', filtro, pagina, tamanhoPagina, filtrosTabela);
}

export async function exportarFretesCsv(filtro: FretesFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await baixarCsv('/api/painel/fretes/exportacao', filtro, 'fretes', filtrosTabela);
}
