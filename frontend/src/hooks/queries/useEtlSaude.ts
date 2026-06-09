import { useQuery } from '@tanstack/react-query';
import {
  buscarEtlSaudeGraficos,
  buscarEtlSaudeOverview,
  buscarEtlSaudeSerie,
  buscarEtlSaudeTabela,
  buscarEtlSaudeTabelaPaginada,
  buscarEtlSaudeTabelaTotal,
} from '../../api/endpoints/etlSaudeServico';
import type { FiltroQuery } from '../../types/common';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useEtlSaudeOverview(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['etl-saude', 'overview', filtro],
    queryFn: () => buscarEtlSaudeOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useEtlSaudeSerie(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['etl-saude', 'serie', filtro],
    queryFn: () => buscarEtlSaudeSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useEtlSaudeGraficos(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['etl-saude', 'graficos', filtro],
    queryFn: () => buscarEtlSaudeGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useEtlSaudeTabela(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['etl-saude', 'tabela', filtro],
    queryFn: () => buscarEtlSaudeTabela(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useEtlSaudeTabelaTotal(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['etl-saude', 'tabela-total', filtro],
    queryFn: () => buscarEtlSaudeTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useEtlSaudeTabelaPaginada(filtro: FiltroQuery, pagina: number, tamanhoPagina: number) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['etl-saude', 'tabela-paginada', filtro, pagina, tamanhoPagina],
    queryFn: () => buscarEtlSaudeTabelaPaginada(filtro, pagina, tamanhoPagina),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
