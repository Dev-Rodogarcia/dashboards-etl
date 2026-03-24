import { useQuery } from '@tanstack/react-query';
import {
  buscarFiliais,
  buscarClientes,
  buscarMotoristas,
  buscarVeiculos,
  buscarPlanoContas,
  buscarUsuarios,
} from '../../api/endpoints/dimensoesServico';

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
