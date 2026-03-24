import { useQuery } from '@tanstack/react-query';
import {
  buscarFaturasAging,
  buscarFaturasMensal,
  buscarFaturasOverview,
  buscarFaturasReconciliacao,
  buscarFaturasStatusProcesso,
  buscarFaturasTabela,
  buscarFaturasTopClientes,
} from '../../api/endpoints/faturasServico';
import type { FaturasFiltro } from '../../types/faturas';

const STALE_TIME = 5 * 60 * 1000;

export function useFaturasOverview(filtro: FaturasFiltro) {
  return useQuery({
    queryKey: ['faturas', 'overview', filtro],
    queryFn: () => buscarFaturasOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasMensal(filtro: FaturasFiltro) {
  return useQuery({
    queryKey: ['faturas', 'mensal', filtro],
    queryFn: () => buscarFaturasMensal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasAging(filtro: FaturasFiltro) {
  return useQuery({
    queryKey: ['faturas', 'aging', filtro],
    queryFn: () => buscarFaturasAging(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasTopClientes(filtro: FaturasFiltro, limite = 10) {
  return useQuery({
    queryKey: ['faturas', 'top-clientes', filtro, limite],
    queryFn: () => buscarFaturasTopClientes(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasStatusProcesso(filtro: FaturasFiltro) {
  return useQuery({
    queryKey: ['faturas', 'status-processo', filtro],
    queryFn: () => buscarFaturasStatusProcesso(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasReconciliacao(filtro: FaturasFiltro, limite = 50) {
  return useQuery({
    queryKey: ['faturas', 'reconciliacao', filtro, limite],
    queryFn: () => buscarFaturasReconciliacao(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasTabela(filtro: FaturasFiltro, limite = 100) {
  return useQuery({
    queryKey: ['faturas', 'tabela', filtro, limite],
    queryFn: () => buscarFaturasTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
