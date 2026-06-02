import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  atualizarKpiGoalsFilial,
  atualizarKpiGoalsGlobais,
  buscarCubagemMercadoriasOverview,
  buscarCubagemMercadoriasSerie,
  buscarCubagemMercadoriasTabela,
  buscarCubagemMercadoriasTabelaPaginada,
  buscarHorariosCorteOverview,
  buscarHorariosCorteSerie,
  buscarHorariosCorteTabela,
  buscarHorariosCorteTabelaPaginada,
  buscarKpiGoalsCompleto,
  buscarKpiGoalsEfetivos,
  buscarKpiGoalsHistoricoPaginado,
  buscarKpiGoalOverrides,
  buscarIndenizacaoMercadoriasOverview,
  buscarIndenizacaoMercadoriasSerie,
  buscarIndenizacaoMercadoriasTabela,
  buscarIndenizacaoMercadoriasTabelaPaginada,
  buscarPerformanceEntregaOverview,
  buscarPerformanceEntregaSerie,
  buscarPerformanceEntregaTabela,
  buscarPerformanceEntregaTabelaPaginada,
  removerKpiGoalsOverride,
  buscarUtilizacaoColetoresOverview,
  buscarUtilizacaoColetoresRanking,
  buscarUtilizacaoColetoresSerie,
  buscarUtilizacaoColetoresTabela,
  buscarUtilizacaoColetoresTabelaPaginada,
} from '../../api/endpoints/indicadoresGestaoAVistaServico';
import type { IndicadoresGestaoVistaFiltro, KpiGoalIndicatorKey, KpiGoalsUpdatePayload } from '../../types/indicadoresGestaoAVista';

const STALE_TIME = 5 * 60 * 1000;

export function useKpiGoalsEffective(branchId: string, enabled = true) {
  return useQuery({
    queryKey: ['kpi-goals', 'effective', branchId],
    queryFn: () => buscarKpiGoalsEfetivos(branchId),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useKpiGoalsFull(enabled = true) {
  return useQuery({
    queryKey: ['kpi-goals', 'full'],
    queryFn: buscarKpiGoalsCompleto,
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useKpiGoalHistory(branchId: string, pagina = 1, tamanhoPagina = 10, enabled = true) {
  return useQuery({
    queryKey: ['kpi-goals', 'history', branchId, pagina, tamanhoPagina],
    queryFn: () => buscarKpiGoalsHistoricoPaginado(branchId, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: enabled && Boolean(branchId),
  });
}

export function useKpiGoalOverrides(indicatorKey: KpiGoalIndicatorKey, enabled = true) {
  return useQuery({
    queryKey: ['kpi-goals', 'overrides', indicatorKey],
    queryFn: () => buscarKpiGoalOverrides(indicatorKey),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useAtualizarKpiGoalsGlobais() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: KpiGoalsUpdatePayload) => atualizarKpiGoalsGlobais(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kpi-goals'] });
    },
  });
}

export function useAtualizarKpiGoalsFilial() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ branchId, payload }: { branchId: string; payload: KpiGoalsUpdatePayload }) => atualizarKpiGoalsFilial(branchId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kpi-goals'] });
    },
  });
}

export function useRemoverKpiGoalsOverride() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (branchId: string) => removerKpiGoalsOverride(branchId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kpi-goals'] });
    },
  });
}

export function usePerformanceEntregaOverview(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'overview', filtro],
    queryFn: () => buscarPerformanceEntregaOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceEntregaSerie(filtro: IndicadoresGestaoVistaFiltro, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'serie', filtro],
    queryFn: () => buscarPerformanceEntregaSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function usePerformanceEntregaTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'tabela', filtro, limite],
    queryFn: () => buscarPerformanceEntregaTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function usePerformanceEntregaTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarPerformanceEntregaTabelaPaginada(filtro, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useUtilizacaoColetoresOverview(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'overview', filtro],
    queryFn: () => buscarUtilizacaoColetoresOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useUtilizacaoColetoresSerie(filtro: IndicadoresGestaoVistaFiltro, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'serie', filtro],
    queryFn: () => buscarUtilizacaoColetoresSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useUtilizacaoColetoresRanking(filtro: IndicadoresGestaoVistaFiltro, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'ranking', filtro],
    queryFn: () => buscarUtilizacaoColetoresRanking(filtro),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useUtilizacaoColetoresTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'tabela', filtro, limite],
    queryFn: () => buscarUtilizacaoColetoresTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useUtilizacaoColetoresTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarUtilizacaoColetoresTabelaPaginada(filtro, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useCubagemMercadoriasOverview(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'cubagem-mercadorias', 'overview', filtro],
    queryFn: () => buscarCubagemMercadoriasOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCubagemMercadoriasSerie(filtro: IndicadoresGestaoVistaFiltro, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'cubagem-mercadorias', 'serie', filtro],
    queryFn: () => buscarCubagemMercadoriasSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCubagemMercadoriasTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'cubagem-mercadorias', 'tabela', filtro, limite],
    queryFn: () => buscarCubagemMercadoriasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCubagemMercadoriasTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'cubagem-mercadorias', 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarCubagemMercadoriasTabelaPaginada(filtro, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useIndenizacaoMercadoriasOverview(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'indenizacao-mercadorias', 'overview', filtro],
    queryFn: () => buscarIndenizacaoMercadoriasOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useIndenizacaoMercadoriasSerie(filtro: IndicadoresGestaoVistaFiltro, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'indenizacao-mercadorias', 'serie', filtro],
    queryFn: () => buscarIndenizacaoMercadoriasSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useIndenizacaoMercadoriasTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'indenizacao-mercadorias', 'tabela', filtro, limite],
    queryFn: () => buscarIndenizacaoMercadoriasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useIndenizacaoMercadoriasTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'indenizacao-mercadorias', 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarIndenizacaoMercadoriasTabelaPaginada(filtro, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}

export function useHorariosCorteOverview(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'horarios-corte', 'overview', filtro],
    queryFn: () => buscarHorariosCorteOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useHorariosCorteSerie(filtro: IndicadoresGestaoVistaFiltro, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'horarios-corte', 'serie', filtro],
    queryFn: () => buscarHorariosCorteSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useHorariosCorteTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100, enabled = true) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'horarios-corte', 'tabela', filtro, limite],
    queryFn: () => buscarHorariosCorteTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useHorariosCorteTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'horarios-corte', 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarHorariosCorteTabelaPaginada(filtro, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: false,
    refetchOnWindowFocus: false,
    enabled,
  });
}
