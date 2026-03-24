import { useQuery } from '@tanstack/react-query';
import {
  buscarContasAPagarGraficos,
  buscarContasAPagarOverview,
  buscarContasAPagarSerie,
  buscarContasAPagarTabela,
} from '../../api/endpoints/contasAPagarServico';
import type { ContasAPagarFiltro } from '../../types/contasAPagar';

const STALE_TIME = 5 * 60 * 1000;

export function useContasAPagarOverview(filtro: ContasAPagarFiltro) {
  return useQuery({
    queryKey: ['contas-a-pagar', 'overview', filtro],
    queryFn: () => buscarContasAPagarOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarSerie(filtro: ContasAPagarFiltro) {
  return useQuery({
    queryKey: ['contas-a-pagar', 'serie', filtro],
    queryFn: () => buscarContasAPagarSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarGraficos(filtro: ContasAPagarFiltro) {
  return useQuery({
    queryKey: ['contas-a-pagar', 'graficos', filtro],
    queryFn: () => buscarContasAPagarGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarTabela(filtro: ContasAPagarFiltro, limite = 100) {
  return useQuery({
    queryKey: ['contas-a-pagar', 'tabela', filtro, limite],
    queryFn: () => buscarContasAPagarTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
