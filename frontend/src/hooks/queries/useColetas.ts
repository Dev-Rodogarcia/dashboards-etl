import { useQuery } from '@tanstack/react-query';
import {
  buscarColetasGraficos,
  buscarColetasOverview,
  buscarColetasSerie,
  buscarColetasTabela,
  buscarColetasTabelaPaginada,
  buscarColetasTabelaTotal,
} from '../../api/endpoints/coletasServico';
import type { ColetasFiltro } from '../../types/coletas';
import type { TableApiFilters } from '../../types/tableFilters';

const STALE_TIME = 5 * 60 * 1000;

export function useColetasOverview(filtro: ColetasFiltro) {
  return useQuery({
    queryKey: ['coletas', 'overview', filtro],
    queryFn: () => buscarColetasOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasSerie(filtro: ColetasFiltro) {
  return useQuery({
    queryKey: ['coletas', 'serie', filtro],
    queryFn: () => buscarColetasSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasGraficos(filtro: ColetasFiltro) {
  return useQuery({
    queryKey: ['coletas', 'graficos', filtro],
    queryFn: () => buscarColetasGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasTabela(filtro: ColetasFiltro, limite = 100) {
  return useQuery({
    queryKey: ['coletas', 'tabela', filtro, limite],
    queryFn: () => buscarColetasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useColetasTabelaTotal(filtro: ColetasFiltro) {
  return useQuery({
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
    queryKey: ['coletas', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarColetasTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
