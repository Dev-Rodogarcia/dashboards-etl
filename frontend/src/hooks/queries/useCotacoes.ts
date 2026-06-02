import { useQuery } from '@tanstack/react-query';
import {
  buscarCotacoesGraficos,
  buscarCotacoesOverview,
  buscarCotacoesSerie,
  buscarCotacoesTabela,
  buscarCotacoesTabelaPaginada,
  buscarCotacoesTabelaTotal,
} from '../../api/endpoints/cotacoesServico';
import type { CotacoesFiltro } from '../../types/cotacoes';
import type { TableApiFilters } from '../../types/tableFilters';

const STALE_TIME = 5 * 60 * 1000;

export function useCotacoesOverview(filtro: CotacoesFiltro) {
  return useQuery({
    queryKey: ['cotacoes', 'overview', filtro],
    queryFn: () => buscarCotacoesOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCotacoesSerie(filtro: CotacoesFiltro, enabled = true) {
  return useQuery({
    queryKey: ['cotacoes', 'serie', filtro],
    queryFn: () => buscarCotacoesSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesGraficos(filtro: CotacoesFiltro, enabled = true) {
  return useQuery({
    queryKey: ['cotacoes', 'graficos', filtro],
    queryFn: () => buscarCotacoesGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesTabela(filtro: CotacoesFiltro, limite = 100, enabled = true) {
  return useQuery({
    queryKey: ['cotacoes', 'tabela', filtro, limite],
    queryFn: () => buscarCotacoesTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesTabelaTotal(filtro: CotacoesFiltro, enabled = true) {
  return useQuery({
    queryKey: ['cotacoes', 'tabela-total', filtro],
    queryFn: () => buscarCotacoesTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesTabelaPaginada(
  filtro: CotacoesFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
  enabled = true,
) {
  return useQuery({
    queryKey: ['cotacoes', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarCotacoesTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}
