import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  buscarFretesGraficos,
  buscarFretesMetas,
  buscarFretesMetasConfiguracoes,
  buscarFretesMixDocumental,
  buscarFretesOverview,
  buscarFretesSerie,
  buscarFretesTabela,
  buscarFretesTabelaPaginada,
  buscarFretesTabelaTotal,
  buscarFretesTopClientes,
  replicarFretesMetasConfiguracoes,
  removerFretesMetaConfiguracao,
  salvarFretesMetaConfiguracao,
} from '../../api/endpoints/fretesServico';
import type { FretesFiltro, FretesGoalConfigPayload, FretesGoalReplicarPayload } from '../../types/fretes';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useFretesOverview(filtro: FretesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'overview', filtro],
    queryFn: () => buscarFretesOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesSerie(filtro: FretesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'serie', filtro],
    queryFn: () => buscarFretesSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesTopClientes(filtro: FretesFiltro, limite = 10) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'top-clientes', filtro, limite],
    queryFn: () => buscarFretesTopClientes(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesMixDocumental(filtro: FretesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'mix-documental', filtro],
    queryFn: () => buscarFretesMixDocumental(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesGraficos(filtro: FretesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'graficos', filtro],
    queryFn: () => buscarFretesGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesMetas(filtro: FretesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'metas', filtro],
    queryFn: () => buscarFretesMetas(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesMetasConfiguracoes(ano: number, mes: number, enabled = true) {
  return useQuery({
    queryKey: ['fretes', 'metas-configuracoes', ano, mes],
    queryFn: () => buscarFretesMetasConfiguracoes(ano, mes),
    staleTime: STALE_TIME,
    retry: false,
    enabled,
  });
}

export function useSalvarFretesMetaConfiguracao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: FretesGoalConfigPayload) => salvarFretesMetaConfiguracao(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fretes'] });
    },
  });
}

export function useReplicarFretesMetasConfiguracoes() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: FretesGoalReplicarPayload) => replicarFretesMetasConfiguracoes(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fretes'] });
    },
  });
}

export function useRemoverFretesMetaConfiguracao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ branchId, ano, mes }: { branchId: string; ano: number; mes: number }) => removerFretesMetaConfiguracao(branchId, ano, mes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fretes'] });
    },
  });
}

export function useFretesTabela(filtro: FretesFiltro, limite = 100) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'tabela', filtro, limite],
    queryFn: () => buscarFretesTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesTabelaTotal(filtro: FretesFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'tabela-total', filtro],
    queryFn: () => buscarFretesTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFretesTabelaPaginada(
  filtro: FretesFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['fretes', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarFretesTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
