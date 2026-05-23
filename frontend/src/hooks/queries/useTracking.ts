import { useQuery } from '@tanstack/react-query';
import {
  buscarTrackingGraficos,
  buscarTrackingDashboard,
  buscarTrackingDetalhesPaginada,
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

export function useTrackingDashboard(filtro: TrackingFiltro, enabled = true) {
  return useQuery({
    queryKey: ['tracking', 'dashboard', filtro],
    queryFn: () => buscarTrackingDashboard(filtro),
    enabled,
    placeholderData: (previousData) => previousData,
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
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useTrackingDetalhesPaginada(
  filtro: TrackingFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
  enabled = true,
) {
  return useQuery({
    queryKey: ['tracking', 'detalhes', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarTrackingDetalhesPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    enabled,
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
