import { useQuery } from '@tanstack/react-query';
import {
  buscarFiliais,
  buscarClientes,
  buscarPagadores,
  buscarFaturasPorClienteClientesCnpj,
  buscarMotoristas,
  buscarVeiculos,
  buscarPlanoContas,
  buscarUsuarios,
  buscarCotacoesUsuarios,
  buscarFaturamentoStatus,
  buscarFaturamentoResponsaveis,
  buscarFretesStatus,
  buscarPerformanceCidadesDestino,
  buscarPerformanceRegioesDestino,
  buscarPerformanceResponsaveis,
} from '../../api/endpoints/dimensoesServico';
import type { FaturamentoFiltro } from '../../types/faturamento';
import type { CotacoesFiltro } from '../../types/cotacoes';
import type { FretesFiltro } from '../../types/fretes';
import type { PerformanceFiltro } from '../../types/performance';

const STALE_TIME = 30 * 60 * 1000; // 30 minutos
const GC_TIME = 24 * 60 * 60 * 1000; // 24 horas

export function useFiliais() {
  return useQuery({
    queryKey: ['dim', 'filiais'],
    queryFn: buscarFiliais,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useClientes() {
  return useQuery({
    queryKey: ['dim', 'clientes'],
    queryFn: buscarClientes,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function usePagadores(busca: string) {
  const buscaNormalizada = busca.trim();

  return useQuery({
    queryKey: ['dim', 'pagadores', buscaNormalizada],
    queryFn: () => buscarPagadores(buscaNormalizada),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteClientesCnpj() {
  return useQuery({
    queryKey: ['dim', 'faturas-por-cliente', 'clientes-cnpj'],
    queryFn: buscarFaturasPorClienteClientesCnpj,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useMotoristas() {
  return useQuery({
    queryKey: ['dim', 'motoristas'],
    queryFn: buscarMotoristas,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useVeiculos() {
  return useQuery({
    queryKey: ['dim', 'veiculos'],
    queryFn: buscarVeiculos,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function usePlanoContas() {
  return useQuery({
    queryKey: ['dim', 'planocontas'],
    queryFn: buscarPlanoContas,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useUsuarios() {
  return useQuery({
    queryKey: ['dim', 'usuarios'],
    queryFn: buscarUsuarios,
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useFretesStatus(filtro: FretesFiltro) {
  return useQuery({
    queryKey: ['dim', 'fretes', 'status', filtro],
    queryFn: () => buscarFretesStatus(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useFaturamentoStatus(filtro: FaturamentoFiltro) {
  return useQuery({
    queryKey: ['dim', 'faturamento', 'status', filtro],
    queryFn: () => buscarFaturamentoStatus(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function usePerformanceResponsaveis(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: ['dim', 'performance', 'responsaveis', filtro],
    queryFn: () => buscarPerformanceResponsaveis(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function usePerformanceRegioesDestino(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: ['dim', 'performance', 'regioes-destino', filtro],
    queryFn: () => buscarPerformanceRegioesDestino(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function usePerformanceCidadesDestino(filtro: PerformanceFiltro) {
  return useQuery({
    queryKey: ['dim', 'performance', 'cidades-destino', filtro],
    queryFn: () => buscarPerformanceCidadesDestino(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useFaturamentoResponsaveis(filtro: FaturamentoFiltro) {
  return useQuery({
    queryKey: ['dim', 'faturamento', 'responsaveis', filtro],
    queryFn: () => buscarFaturamentoResponsaveis(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}

export function useCotacoesUsuarios(filtro: CotacoesFiltro) {
  return useQuery({
    queryKey: ['dim', 'cotacoes', 'usuarios', filtro],
    queryFn: () => buscarCotacoesUsuarios(filtro),
    staleTime: STALE_TIME,
    gcTime: GC_TIME,
    retry: 1,
  });
}
