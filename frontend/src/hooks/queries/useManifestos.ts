import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  buscarManifestosGraficos,
  buscarManifestosMetas,
  buscarManifestosOverview,
  buscarManifestosPerformance,
  buscarManifestosSerie,
  buscarManifestosTabela,
  buscarManifestosTabelaPaginada,
  buscarManifestosTabelaTotal,
  removerManifestosMeta,
  salvarManifestosMeta,
} from '../../api/endpoints/manifestosServico';
import type { ManifestosCostGoalPayload, ManifestosFiltro, ManifestosTempoNivel } from '../../types/manifestos';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;
const QUERY_KEY = ['manifestos'];

export function useManifestosOverview(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'overview', filtro],
    queryFn: () => buscarManifestosOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosSerie(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'serie', filtro],
    queryFn: () => buscarManifestosSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosGraficos(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'graficos', filtro],
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
    queryKey: [...QUERY_KEY, 'performance', filtro, nivel, ano, mes],
    queryFn: () => buscarManifestosPerformance(filtro, nivel, ano, mes),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosTabela(filtro: ManifestosFiltro, limite = 100) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'tabela', filtro, limite],
    queryFn: () => buscarManifestosTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosTabelaTotal(filtro: ManifestosFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'tabela-total', filtro],
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
  sortField?: string,
  sortDirection?: 'asc' | 'desc',
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela, sortField, sortDirection],
    queryFn: () => buscarManifestosTabelaPaginada(
      filtro,
      pagina,
      tamanhoPagina,
      filtrosTabela,
      sortField,
      sortDirection,
    ),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosMetas(ano: number, mes: number, enabled = true) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'metas', ano, mes],
    queryFn: () => buscarManifestosMetas(ano, mes),
    staleTime: STALE_TIME,
    retry: false,
    enabled,
  });
}

export function useSaveManifestosMeta() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ManifestosCostGoalPayload) => salvarManifestosMeta(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useDeleteManifestosMeta() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ branchId, ano, mes }: { branchId: string; ano: number; mes: number }) => removerManifestosMeta(branchId, ano, mes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}
