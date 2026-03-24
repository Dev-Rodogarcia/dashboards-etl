import { useQuery } from '@tanstack/react-query';
import {
  buscarFretesGraficos,
  buscarFretesMixDocumental,
  buscarFretesOverview,
  buscarFretesSerie,
  buscarFretesTabela,
  buscarFretesTopClientes,
} from '../../api/endpoints/fretesServico';
import type { FretesFiltro } from '../../types/fretes';

const STALE_TIME = 5 * 60 * 1000;

export function useFretesOverview(filtro: FretesFiltro) {
  return useQuery({
    queryKey: ['fretes', 'overview', filtro],
    queryFn: () => buscarFretesOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesSerie(filtro: FretesFiltro) {
  return useQuery({
    queryKey: ['fretes', 'serie', filtro],
    queryFn: () => buscarFretesSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesTopClientes(filtro: FretesFiltro, limite = 10) {
  return useQuery({
    queryKey: ['fretes', 'top-clientes', filtro, limite],
    queryFn: () => buscarFretesTopClientes(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesMixDocumental(filtro: FretesFiltro) {
  return useQuery({
    queryKey: ['fretes', 'mix-documental', filtro],
    queryFn: () => buscarFretesMixDocumental(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesGraficos(filtro: FretesFiltro) {
  return useQuery({
    queryKey: ['fretes', 'graficos', filtro],
    queryFn: () => buscarFretesGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesTabela(filtro: FretesFiltro, limite = 100) {
  return useQuery({
    queryKey: ['fretes', 'tabela', filtro, limite],
    queryFn: () => buscarFretesTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
