import { useQuery } from '@tanstack/react-query';
import { buscarExecutivoOverview, buscarExecutivoSerie } from '../../api/endpoints/executivoServico';
import type { FiltroQuery } from '../../types/common';

const STALE_TIME = 5 * 60 * 1000;

export function useExecutivoOverview(filtro: FiltroQuery) {
  return useQuery({
    queryKey: ['executivo', 'overview', filtro],
    queryFn: () => buscarExecutivoOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useExecutivoSerie(filtro: FiltroQuery) {
  return useQuery({
    queryKey: ['executivo', 'serie', filtro],
    queryFn: () => buscarExecutivoSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
