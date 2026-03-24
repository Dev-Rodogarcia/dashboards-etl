import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { ColetaResumoRow, ColetasCharts, ColetasFiltro, ColetasOverview, ColetasTrendPoint } from '../../types/coletas';

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

export async function buscarColetasTabela(
  filtro: ColetasFiltro,
  limite = 100
): Promise<ColetaResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<ColetaResumoRow[]>('/api/painel/coletas/tabela', { params });
  return data;
}
