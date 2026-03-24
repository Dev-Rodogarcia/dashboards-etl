import { useQuery } from '@tanstack/react-query';
import { buscarTrackingGraficos, buscarTrackingOverview, buscarTrackingSerie, buscarTrackingTabela } from '../../api/endpoints/trackingServico';
import type { TrackingFiltro } from '../../types/tracking';

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
