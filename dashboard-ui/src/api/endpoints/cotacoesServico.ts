import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { CotacaoResumoRow, CotacoesCharts, CotacoesFiltro, CotacoesOverview, CotacoesTrendPoint } from '../../types/cotacoes';

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
