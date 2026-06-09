import { useQuery } from '@tanstack/react-query';
import { buscarExecutivoOverview, buscarExecutivoResumoFinanceiro, buscarExecutivoSerie } from '../../api/endpoints/executivoServico';
import type { FiltroQuery } from '../../types/common';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useExecutivoOverview(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['executivo', 'overview', filtro],
    queryFn: () => buscarExecutivoOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useExecutivoSerie(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['executivo', 'serie', filtro],
    queryFn: () => buscarExecutivoSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useExecutivoResumoFinanceiro(filtro: FiltroQuery) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['executivo', 'resumo-financeiro', filtro],
    queryFn: () => buscarExecutivoResumoFinanceiro(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
