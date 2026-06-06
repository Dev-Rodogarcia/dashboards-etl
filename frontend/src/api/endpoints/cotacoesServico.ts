import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  CotacaoResumoRow,
  CotacoesCharts,
  CotacoesFiltro,
  CotacoesOverview,
  CotacoesResumoAgregado,
  CotacoesTrendPoint,
} from '../../types/cotacoes';
import type { TableApiFilters } from '../../types/tableFilters';

export async function buscarCotacoesOverview(filtro: CotacoesFiltro): Promise<CotacoesOverview> {
  const { data } = await clienteAxios.get<CotacoesOverview>('/api/painel/cotacoes', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCotacoesSerie(filtro: CotacoesFiltro): Promise<CotacoesTrendPoint[]> {
  const { data } = await clienteAxios.get<CotacoesTrendPoint[]>('/api/painel/cotacoes/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCotacoesGraficos(filtro: CotacoesFiltro): Promise<CotacoesCharts> {
  const { data } = await clienteAxios.get<CotacoesCharts>('/api/painel/cotacoes/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCotacoesTabela(
  filtro: CotacoesFiltro,
  limite = 100
): Promise<CotacaoResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<CotacaoResumoRow[]>('/api/painel/cotacoes/tabela', { params });
  return data;
}

export async function buscarCotacoesResumoUsuario(filtro: CotacoesFiltro): Promise<CotacoesResumoAgregado[]> {
  const { data } = await clienteAxios.get<CotacoesResumoAgregado[]>('/api/painel/cotacoes/resumo/usuario', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCotacoesResumoFilial(filtro: CotacoesFiltro): Promise<CotacoesResumoAgregado[]> {
  const { data } = await clienteAxios.get<CotacoesResumoAgregado[]>('/api/painel/cotacoes/resumo/filial', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCotacoesResumoCliente(filtro: CotacoesFiltro): Promise<CotacoesResumoAgregado[]> {
  const { data } = await clienteAxios.get<CotacoesResumoAgregado[]>('/api/painel/cotacoes/resumo/cliente', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCotacoesTabelaTotal(filtro: CotacoesFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/cotacoes/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarCotacoesTabelaPaginada(
  filtro: CotacoesFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<CotacaoResumoRow>> {
  return buscarTabelaPaginada('/api/painel/cotacoes/tabela/paginada', filtro, pagina, tamanhoPagina, filtrosTabela);
}

export async function exportarCotacoesCsv(filtro: CotacoesFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await baixarCsv('/api/painel/cotacoes/exportacao', filtro, 'cotacoes', filtrosTabela);
}
