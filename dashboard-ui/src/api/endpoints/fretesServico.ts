import clienteAxios from '../clienteAxios';
import { baixarExcel } from '../downloadExcel';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  FretesCharts,
  FretesClienteRanking,
  FretesDocumentMix,
  FretesFiltro,
  FretesOverview,
  FretesTrendPoint,
  FreteResumoRow,
} from '../../types/fretes';

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
): Promise<PaginacaoResponse<FreteResumoRow>> {
  return buscarTabelaPaginada('/api/painel/fretes/tabela/paginada', filtro, pagina, tamanhoPagina);
}

export async function exportarFretesExcel(filtro: FretesFiltro): Promise<void> {
  await baixarExcel('/api/painel/fretes/exportacao', filtro, 'fretes');
}
