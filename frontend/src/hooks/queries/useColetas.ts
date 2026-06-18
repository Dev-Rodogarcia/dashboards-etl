import { useQuery } from '@tanstack/react-query';
import {
  buscarColetasCidadesOrigem,
  buscarColetasGraficos,
  buscarColetasHistoricoPerformance,
  buscarColetasOverview,
  buscarColetasSerie,
  buscarColetasTabela,
  buscarColetasTabelaPaginada,
  buscarColetasTabelaTotal,
} from '../../api/endpoints/coletasServico';
import type { ColetasFiltro, ColetasHistoricoPeriodo } from '../../types/coletas';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useColetasOverview(filtro: ColetasFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'overview', filtro],
    queryFn: () => buscarColetasOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasSerie(filtro: ColetasFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'serie', filtro],
    queryFn: () => buscarColetasSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasGraficos(filtro: ColetasFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'graficos', filtro],
    queryFn: () => buscarColetasGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasHistoricoPerformance(
  filtro: ColetasFiltro,
  historicoPeriodo: ColetasHistoricoPeriodo = 'dias',
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'graficos', 'historico-performance', filtro, historicoPeriodo],
    queryFn: () => buscarColetasHistoricoPerformance(filtro, historicoPeriodo),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasCidadesOrigem(filtro: ColetasFiltro, regiao: string | null) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'graficos', 'cidades-origem', filtro, regiao],
    queryFn: () => buscarColetasCidadesOrigem(filtro, regiao ?? ''),
    enabled: Boolean(regiao),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasTabela(filtro: ColetasFiltro, limite = 100) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'tabela', filtro, limite],
    queryFn: () => buscarColetasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasTabelaTotal(filtro: ColetasFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'tabela-total', filtro],
    queryFn: () => buscarColetasTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasTabelaPaginada(
  filtro: ColetasFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['coletas', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarColetasTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
