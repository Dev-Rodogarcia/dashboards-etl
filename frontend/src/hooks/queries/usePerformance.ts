import { useQuery } from '@tanstack/react-query';
import {
  buscarPerformanceAging,
  buscarPerformanceDrilldown,
  buscarPerformanceHistorico,
  buscarPerformanceOverview,
  buscarPerformanceSerieTemporal,
  buscarPerformanceStatus,
  buscarPerformanceTabelaPaginada,
} from '../../api/endpoints/performanceServico';
import type { PerformanceDrilldownParams, PerformanceFiltro, PerformanceTempoNivel } from '../../types/performance';

const STALE_TIME = 5 * 60 * 1000;
const QUERY_KEY = ['performance'];

export function usePerformanceOverview(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'overview', filtro],
    queryFn: () => buscarPerformanceOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceSerieTemporal(
  filtro: PerformanceFiltro,
  nivel: PerformanceTempoNivel,
  ano?: number | null,
  mes?: number | null,
) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'serie-temporal', filtro, nivel, ano, mes],
    queryFn: () => buscarPerformanceSerieTemporal(filtro, nivel, ano, mes),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceStatus(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'status', filtro],
    queryFn: () => buscarPerformanceStatus(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceHistorico(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'historico', filtro],
    queryFn: () => buscarPerformanceHistorico(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceDrilldown(
  filtro: PerformanceFiltro,
  drilldown: PerformanceDrilldownParams,
) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'drilldown', filtro, drilldown],
    queryFn: () => buscarPerformanceDrilldown(filtro, drilldown),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceAging(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'aging', filtro],
    queryFn: () => buscarPerformanceAging(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceTabelaPaginada(
  filtro: PerformanceFiltro,
  pagina: number,
  tamanhoPagina: number,
) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarPerformanceTabelaPaginada(filtro, pagina, tamanhoPagina),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
