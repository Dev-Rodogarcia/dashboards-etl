import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { ManifestoResumoRow, ManifestosCharts, ManifestosFiltro, ManifestosOverview, ManifestosTrendPoint } from '../../types/manifestos';

export async function buscarManifestosOverview(filtro: ManifestosFiltro): Promise<ManifestosOverview> {
  const { data } = await clienteAxios.get<ManifestosOverview>('/api/painel/manifestos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarManifestosSerie(filtro: ManifestosFiltro): Promise<ManifestosTrendPoint[]> {
  const { data } = await clienteAxios.get<ManifestosTrendPoint[]>('/api/painel/manifestos/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarManifestosGraficos(filtro: ManifestosFiltro): Promise<ManifestosCharts> {
  const { data } = await clienteAxios.get<ManifestosCharts>('/api/painel/manifestos/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarManifestosTabela(
  filtro: ManifestosFiltro,
  limite = 100
): Promise<ManifestoResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<ManifestoResumoRow[]>('/api/painel/manifestos/tabela', { params });
  return data;
}
