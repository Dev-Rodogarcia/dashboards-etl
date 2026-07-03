import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type { ColetaResumoRow, ColetasCharts, ColetasCidadeOrigem, ColetasFiltro, ColetasHistoricoPerformance, ColetasHistoricoPeriodo, ColetasOverview, ColetasTrendPoint } from '../../types/coletas';
import type { TableApiFilters } from '../../types/tableFilters';

export async function buscarColetasOverview(filtro: ColetasFiltro): Promise<ColetasOverview> {
  const { data } = await clienteAxios.get<ColetasOverview>('/api/painel/coletas', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarColetasSerie(filtro: ColetasFiltro): Promise<ColetasTrendPoint[]> {
  const { data } = await clienteAxios.get<ColetasTrendPoint[]>('/api/painel/coletas/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarColetasGraficos(filtro: ColetasFiltro): Promise<ColetasCharts> {
  const { data } = await clienteAxios.get<ColetasCharts>('/api/painel/coletas/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarColetasHistoricoPerformance(
  filtro: ColetasFiltro,
  periodo: ColetasHistoricoPeriodo = 'dias',
): Promise<ColetasHistoricoPerformance[]> {
  const params = montarQueryParams(filtro);
  params.set('periodo', periodo);
  const { data } = await clienteAxios.get<ColetasHistoricoPerformance[]>('/api/painel/coletas/graficos/historico-performance', {
    params,
  });
  return data;
}

export async function buscarColetasCidadesOrigem(filtro: ColetasFiltro, regiaoLogistica: string): Promise<ColetasCidadeOrigem[]> {
  const params = montarQueryParams(filtro);
  params.set('regiaoLogistica', regiaoLogistica);
  const { data } = await clienteAxios.get<ColetasCidadeOrigem[]>('/api/painel/coletas/graficos/cidades', {
    params,
  });
  return data;
}

export async function buscarColetasTabela(
  filtro: ColetasFiltro,
  limite = 100
): Promise<ColetaResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<ColetaResumoRow[]>('/api/painel/coletas/tabela', { params });
  return data;
}

export async function buscarColetasTabelaTotal(filtro: ColetasFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/coletas/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarColetasTabelaPaginada(
  filtro: ColetasFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<ColetaResumoRow>> {
  return buscarTabelaPaginada('/api/painel/coletas/tabela/paginada', filtro, pagina, tamanhoPagina, filtrosTabela);
}

export async function exportarColetasCsv(filtro: ColetasFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await baixarCsv('/api/painel/coletas/exportacao', filtro, 'coletas', filtrosTabela);
}
