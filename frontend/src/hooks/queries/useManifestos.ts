import { useQuery } from '@tanstack/react-query';
import {
  buscarManifestosGraficos,
  buscarManifestosOverview,
  buscarManifestosPerformance,
  buscarManifestosSerie,
  buscarManifestosTabela,
  buscarManifestosTabelaPaginada,
  buscarManifestosTabelaTotal,
} from '../../api/endpoints/manifestosServico';
import type { ManifestosFiltro, ManifestosTempoNivel } from '../../types/manifestos';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useManifestosOverview(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'overview', filtro],
    queryFn: () => buscarManifestosOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosSerie(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'serie', filtro],
    queryFn: () => buscarManifestosSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosGraficos(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'graficos', filtro],
    queryFn: () => buscarManifestosGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosPerformance(
  filtro: ManifestosFiltro,
  nivel: ManifestosTempoNivel,
  ano?: number | null,
  mes?: number | null,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'performance', filtro, nivel, ano, mes],
    queryFn: () => buscarManifestosPerformance(filtro, nivel, ano, mes),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosTabela(filtro: ManifestosFiltro, limite = 100) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'tabela', filtro, limite],
    queryFn: () => buscarManifestosTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosTabelaTotal(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'tabela-total', filtro],
    queryFn: () => buscarManifestosTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosTabelaPaginada(
  filtro: ManifestosFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['manifestos', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarManifestosTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
