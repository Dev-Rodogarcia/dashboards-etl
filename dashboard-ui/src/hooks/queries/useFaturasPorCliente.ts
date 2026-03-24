import { useQuery } from '@tanstack/react-query';
import {
  buscarFaturasPorClienteAging,
  buscarFaturasPorClienteMensal,
  buscarFaturasPorClienteOverview,
  buscarFaturasPorClienteStatusProcesso,
  buscarFaturasPorClienteTabela,
  buscarFaturasPorClienteTopClientes,
} from '../../api/endpoints/faturasPorClienteServico';
import type { FaturasPorClienteFiltro } from '../../types/faturasPorCliente';

const STALE_TIME = 5 * 60 * 1000;

export function useFaturasPorClienteOverview(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    queryKey: ['faturas-por-cliente', 'overview', filtro],
    queryFn: () => buscarFaturasPorClienteOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteMensal(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    queryKey: ['faturas-por-cliente', 'mensal', filtro],
    queryFn: () => buscarFaturasPorClienteMensal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteAging(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    queryKey: ['faturas-por-cliente', 'aging', filtro],
    queryFn: () => buscarFaturasPorClienteAging(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTopClientes(filtro: FaturasPorClienteFiltro, limite = 10) {
  return useQuery({
    queryKey: ['faturas-por-cliente', 'top-clientes', filtro, limite],
    queryFn: () => buscarFaturasPorClienteTopClientes(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteStatusProcesso(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    queryKey: ['faturas-por-cliente', 'status-processo', filtro],
    queryFn: () => buscarFaturasPorClienteStatusProcesso(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTabela(filtro: FaturasPorClienteFiltro, limite = 100) {
  return useQuery({
    queryKey: ['faturas-por-cliente', 'tabela', filtro, limite],
    queryFn: () => buscarFaturasPorClienteTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
