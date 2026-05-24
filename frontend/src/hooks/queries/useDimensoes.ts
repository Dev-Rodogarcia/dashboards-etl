import { useQuery } from '@tanstack/react-query';
import {
  buscarFiliais,
  buscarClientes,
  buscarFaturasPorClienteClientesCnpj,
  buscarMotoristas,
  buscarVeiculos,
  buscarPlanoContas,
  buscarUsuarios,
  buscarFaturamentoStatus,
  buscarFretesStatus,
} from '../../api/endpoints/dimensoesServico';
import type { FaturamentoFiltro } from '../../types/faturamento';
import type { FretesFiltro } from '../../types/fretes';

const STALE_TIME = 30 * 60 * 1000; // 30 minutos

export function useFiliais() {
  return useQuery({
    queryKey: ['dim', 'filiais'],
    queryFn: buscarFiliais,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useClientes() {
  return useQuery({
    queryKey: ['dim', 'clientes'],
    queryFn: buscarClientes,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteClientesCnpj() {
  return useQuery({
    queryKey: ['dim', 'faturas-por-cliente', 'clientes-cnpj'],
    queryFn: buscarFaturasPorClienteClientesCnpj,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useMotoristas() {
  return useQuery({
    queryKey: ['dim', 'motoristas'],
    queryFn: buscarMotoristas,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useVeiculos() {
  return useQuery({
    queryKey: ['dim', 'veiculos'],
    queryFn: buscarVeiculos,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function usePlanoContas() {
  return useQuery({
    queryKey: ['dim', 'planocontas'],
    queryFn: buscarPlanoContas,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useUsuarios() {
  return useQuery({
    queryKey: ['dim', 'usuarios'],
    queryFn: buscarUsuarios,
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesStatus(filtro: FretesFiltro) {
  return useQuery({
    queryKey: ['dim', 'fretes', 'status', filtro],
    queryFn: () => buscarFretesStatus(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturamentoStatus(filtro: FaturamentoFiltro) {
  return useQuery({
    queryKey: ['dim', 'faturamento', 'status', filtro],
    queryFn: () => buscarFaturamentoStatus(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
