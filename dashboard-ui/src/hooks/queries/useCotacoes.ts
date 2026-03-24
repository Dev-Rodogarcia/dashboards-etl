import { useQuery } from '@tanstack/react-query';
import { buscarCotacoesGraficos, buscarCotacoesOverview, buscarCotacoesSerie, buscarCotacoesTabela } from '../../api/endpoints/cotacoesServico';
import type { CotacoesFiltro } from '../../types/cotacoes';

const STALE_TIME = 5 * 60 * 1000;

export function useCotacoesOverview(filtro: CotacoesFiltro) {
  return useQuery({
    queryKey: ['cotacoes', 'overview', filtro],
    queryFn: () => buscarCotacoesOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCotacoesSerie(filtro: CotacoesFiltro) {
  return useQuery({
    queryKey: ['cotacoes', 'serie', filtro],
    queryFn: () => buscarCotacoesSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCotacoesGraficos(filtro: CotacoesFiltro) {
  return useQuery({
    queryKey: ['cotacoes', 'graficos', filtro],
    queryFn: () => buscarCotacoesGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCotacoesTabela(filtro: CotacoesFiltro, limite = 100) {
  return useQuery({
    queryKey: ['cotacoes', 'tabela', filtro, limite],
    queryFn: () => buscarCotacoesTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
