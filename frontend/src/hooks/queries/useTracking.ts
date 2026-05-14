import { useQuery } from '@tanstack/react-query';
import {
  buscarTrackingGraficos,
  buscarTrackingOverview,
  buscarTrackingSerie,
  buscarTrackingTabela,
  buscarTrackingTabelaPaginada,
  buscarTrackingTabelaTotal,
} from '../../api/endpoints/trackingServico';
import type { TrackingFiltro } from '../../types/tracking';
import type { TableApiFilters } from '../../types/tableFilters';

const STALE_TIME = 5 * 60 * 1000;

export function useTrackingOverview(filtro: TrackingFiltro) {
  return useQuery({
    queryKey: ['tracking', 'overview', filtro],
    queryFn: () => buscarTrackingOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useTrackingSerie(filtro: TrackingFiltro) {
  return useQuery({
    queryKey: ['tracking', 'serie', filtro],
    queryFn: () => buscarTrackingSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useTrackingGraficos(filtro: TrackingFiltro) {
  return useQuery({
    queryKey: ['tracking', 'graficos', filtro],
    queryFn: () => buscarTrackingGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useTrackingTabela(filtro: TrackingFiltro, limite = 100) {
  return useQuery({
    queryKey: ['tracking', 'tabela', filtro, limite],
    queryFn: () => buscarTrackingTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useTrackingTabelaTotal(filtro: TrackingFiltro) {
  return useQuery({
    queryKey: ['tracking', 'tabela-total', filtro],
    queryFn: () => buscarTrackingTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useTrackingTabelaPaginada(
  filtro: TrackingFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
) {
  return useQuery({
    queryKey: ['tracking', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarTrackingTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
