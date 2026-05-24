import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  ManifestoResumoRow,
  ManifestosCharts,
  ManifestosFiltro,
  ManifestosOverview,
  ManifestosTempoNivel,
  ManifestosTrendPoint,
  PerformanceVeiculosDados,
} from '../../types/manifestos';
import type { TableApiFilters } from '../../types/tableFilters';

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

export async function buscarManifestosPerformance(
  filtro: ManifestosFiltro,
  nivel: ManifestosTempoNivel,
  ano?: number | null,
  mes?: number | null,
): Promise<PerformanceVeiculosDados> {
  const params = montarQueryParams(filtro);
  params.set('nivel', nivel);
  if (ano) {
    params.set('ano', String(ano));
  }
  if (mes) {
    params.set('mes', String(mes));
  }

  const { data } = await clienteAxios.get<PerformanceVeiculosDados>('/api/painel/manifestos/performance', {
    params,
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

export async function buscarManifestosTabelaTotal(filtro: ManifestosFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/manifestos/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarManifestosTabelaPaginada(
  filtro: ManifestosFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<ManifestoResumoRow>> {
  return buscarTabelaPaginada('/api/painel/manifestos/tabela/paginada', filtro, pagina, tamanhoPagina, filtrosTabela);
}

export async function exportarManifestosCsv(filtro: ManifestosFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await baixarCsv('/api/painel/manifestos/exportacao', filtro, 'manifestos', filtrosTabela);
}
