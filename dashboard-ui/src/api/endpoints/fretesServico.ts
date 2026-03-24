import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
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
