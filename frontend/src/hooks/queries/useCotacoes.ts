import { useQuery } from '@tanstack/react-query';
import {
  buscarCotacoesGraficos,
  buscarCotacoesOverview,
  buscarCotacoesResumoCliente,
  buscarCotacoesResumoFilial,
  buscarCotacoesResumoUsuario,
  buscarCotacoesSerie,
  buscarCotacoesTabela,
  buscarCotacoesTabelaPaginada,
  buscarCotacoesTabelaTotal,
} from '../../api/endpoints/cotacoesServico';
import type { CotacoesFiltro } from '../../types/cotacoes';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

function hasPeriodoValido(filtro: CotacoesFiltro) {
  return Boolean(filtro.dataInicio && filtro.dataFim);
}

export function useCotacoesOverview(filtro: CotacoesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'overview', filtro],
    queryFn: () => buscarCotacoesOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useCotacoesSerie(filtro: CotacoesFiltro, enabled = true) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'serie', filtro],
    queryFn: () => buscarCotacoesSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesGraficos(filtro: CotacoesFiltro, enabled = true) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'graficos', filtro],
    queryFn: () => buscarCotacoesGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesTabela(filtro: CotacoesFiltro, limite = 100, enabled = true) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'tabela', filtro, limite],
    queryFn: () => buscarCotacoesTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}

export function useCotacoesResumoUsuario(filtro: CotacoesFiltro, isActive: boolean) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'resumo', 'usuario', filtro],
    queryFn: () => buscarCotacoesResumoUsuario(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled: isActive && hasPeriodoValido(filtro),
  });
}

export function useCotacoesResumoFilial(filtro: CotacoesFiltro, isActive: boolean) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'resumo', 'filial', filtro],
    queryFn: () => buscarCotacoesResumoFilial(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled: isActive && hasPeriodoValido(filtro),
  });
}

export function useCotacoesResumoCliente(filtro: CotacoesFiltro, isActive: boolean) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'resumo', 'cliente', filtro],
    queryFn: () => buscarCotacoesResumoCliente(filtro),
    staleTime: STALE_TIME,
    retry: 1,
    enabled: isActive && hasPeriodoValido(filtro),
  });
}

export function useCotacoesTabelaTotal(filtro: CotacoesFiltro, enabled = true) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
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
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['cotacoes', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarCotacoesTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
    enabled,
  });
}
