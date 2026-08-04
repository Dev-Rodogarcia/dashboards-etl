import { useQuery } from '@tanstack/react-query';
import {
  buscarContasAPagarGraficos,
  buscarContasAPagarDrilldownCentroCusto,
  buscarContasAPagarDrilldownFornecedores,
  buscarContasAPagarOverview,
  buscarContasAPagarSerie,
  buscarContasAPagarTabela,
  buscarContasAPagarTabelaPaginada,
  buscarContasAPagarTabelaTotal,
} from '../../api/endpoints/contasAPagarServico';
import type { ContasAPagarDrilldownRequest, ContasAPagarFiltro, ContasAPagarGranularidade, ContasAPagarReferenciaTemporal } from '../../types/contasAPagar';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useContasAPagarOverview(filtro: ContasAPagarFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'overview', filtro],
    queryFn: () => buscarContasAPagarOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarSerie(
  filtro: ContasAPagarFiltro,
  granularidade: ContasAPagarGranularidade = 'mes',
  referencia: ContasAPagarReferenciaTemporal = 'emissao',
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'serie', filtro, granularidade, referencia],
    queryFn: () => buscarContasAPagarSerie(filtro, granularidade, referencia),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarDrilldownFornecedores(filtro: ContasAPagarFiltro, request: ContasAPagarDrilldownRequest) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'graficos', 'fornecedores', filtro, request],
    queryFn: () => buscarContasAPagarDrilldownFornecedores(filtro, request),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarDrilldownCentroCusto(filtro: ContasAPagarFiltro, request: ContasAPagarDrilldownRequest) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'graficos', 'centros-custo', filtro, request],
    queryFn: () => buscarContasAPagarDrilldownCentroCusto(filtro, request),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarGraficos(filtro: ContasAPagarFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'graficos', filtro],
    queryFn: () => buscarContasAPagarGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarTabela(filtro: ContasAPagarFiltro, limite = 100) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'tabela', filtro, limite],
    queryFn: () => buscarContasAPagarTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarTabelaTotal(filtro: ContasAPagarFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'tabela-total', filtro],
    queryFn: () => buscarContasAPagarTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useContasAPagarTabelaPaginada(
  filtro: ContasAPagarFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['contas-a-pagar', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarContasAPagarTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
