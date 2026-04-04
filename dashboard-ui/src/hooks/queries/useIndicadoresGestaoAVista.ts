import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  buscarCubagemMercadoriasOverview,
  buscarCubagemMercadoriasSerie,
  buscarCubagemMercadoriasTabela,
  buscarHorariosCorteOverview,
  buscarHorariosCorteSerie,
  buscarHorariosCorteTabela,
  buscarIndenizacaoMercadoriasOverview,
  buscarIndenizacaoMercadoriasSerie,
  buscarIndenizacaoMercadoriasTabela,
  importarHorariosCorte,
  buscarPerformanceEntregaOverview,
  buscarPerformanceEntregaSerie,
  buscarPerformanceEntregaTabela,
  buscarUtilizacaoColetoresOverview,
  buscarUtilizacaoColetoresSerie,
  buscarUtilizacaoColetoresTabela,
} from '../../api/endpoints/indicadoresGestaoAVistaServico';
import type { IndicadoresGestaoVistaFiltro } from '../../types/indicadoresGestaoAVista';

const STALE_TIME = 5 * 60 * 1000;

export function usePerformanceEntregaOverview(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'overview', filtro],
    queryFn: () => buscarPerformanceEntregaOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceEntregaSerie(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'serie', filtro],
    queryFn: () => buscarPerformanceEntregaSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePerformanceEntregaTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'performance-entrega', 'tabela', filtro, limite],
    queryFn: () => buscarPerformanceEntregaTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
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

export function useUtilizacaoColetoresSerie(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'serie', filtro],
    queryFn: () => buscarUtilizacaoColetoresSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useUtilizacaoColetoresTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'utilizacao-coletores', 'tabela', filtro, limite],
    queryFn: () => buscarUtilizacaoColetoresTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
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

export function useCubagemMercadoriasSerie(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'cubagem-mercadorias', 'serie', filtro],
    queryFn: () => buscarCubagemMercadoriasSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCubagemMercadoriasTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'cubagem-mercadorias', 'tabela', filtro, limite],
    queryFn: () => buscarCubagemMercadoriasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
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

export function useIndenizacaoMercadoriasSerie(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'indenizacao-mercadorias', 'serie', filtro],
    queryFn: () => buscarIndenizacaoMercadoriasSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useIndenizacaoMercadoriasTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'indenizacao-mercadorias', 'tabela', filtro, limite],
    queryFn: () => buscarIndenizacaoMercadoriasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
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

export function useHorariosCorteSerie(filtro: IndicadoresGestaoVistaFiltro) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'horarios-corte', 'serie', filtro],
    queryFn: () => buscarHorariosCorteSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useHorariosCorteTabela(filtro: IndicadoresGestaoVistaFiltro, limite = 100) {
  return useQuery({
    queryKey: ['indicadores-gestao-a-vista', 'horarios-corte', 'tabela', filtro, limite],
    queryFn: () => buscarHorariosCorteTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useImportarHorariosCorte() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: importarHorariosCorte,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['indicadores-gestao-a-vista', 'horarios-corte'],
      });
    },
  });
}
