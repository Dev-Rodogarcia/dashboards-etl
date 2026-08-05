import { useQuery } from '@tanstack/react-query';
import {
  buscarFaturasPorClienteAging,
  buscarFaturasPorClienteAgingDrilldown,
  buscarFaturasPorClienteMensal,
  buscarFaturasPorClienteSerie,
  buscarFaturasPorClienteOverview,
  buscarFaturasPorClienteStatusProcesso,
  buscarFaturasPorClienteStatusEvolucao,
  buscarFaturasPorClienteTabela,
  buscarFaturasPorClienteTabelaPaginada,
  buscarFaturasPorClienteTabelaTotal,
  buscarFaturasPorClienteTopClientes,
  buscarFaturasPorClienteTopClientesDrilldown,
} from '../../api/endpoints/faturasPorClienteServico';
import type {
  FaturasPorClienteAgingEscopo,
  FaturasPorClienteDrilldownNivel,
  FaturasPorClienteFiltro,
  FaturasPorClienteGranularidade,
  FaturasPorClienteMetrica,
  FaturasPorClienteReferenciaTemporal,
} from '../../types/faturasPorCliente';
import type { TableApiFilters } from '../../types/tableFilters';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../utils/pollingUtils';

const STALE_TIME = 5 * 60 * 1000;

export function useFaturasPorClienteOverview(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'overview', filtro],
    queryFn: () => buscarFaturasPorClienteOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteMensal(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'mensal', filtro],
    queryFn: () => buscarFaturasPorClienteMensal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteSerie(
  filtro: FaturasPorClienteFiltro,
  granularidade: FaturasPorClienteGranularidade,
  referencia: FaturasPorClienteReferenciaTemporal,
  metrica: FaturasPorClienteMetrica,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'serie', filtro, granularidade, referencia, metrica],
    queryFn: () => buscarFaturasPorClienteSerie(filtro, granularidade, referencia, metrica),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteAging(filtro: FaturasPorClienteFiltro, escopo: FaturasPorClienteAgingEscopo = 'todos') {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'aging', filtro, escopo],
    queryFn: () => buscarFaturasPorClienteAging(filtro, escopo),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteAgingDrilldown(
  filtro: FaturasPorClienteFiltro,
  faixa: string | null,
  nivel: FaturasPorClienteDrilldownNivel,
  cliente: string | null,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'aging', 'drilldown', filtro, faixa, nivel, cliente],
    queryFn: () => buscarFaturasPorClienteAgingDrilldown(filtro, faixa ?? '', nivel, cliente),
    enabled: Boolean(faixa),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTopClientes(filtro: FaturasPorClienteFiltro, limite = 10) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'top-clientes', filtro, limite],
    queryFn: () => buscarFaturasPorClienteTopClientes(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTopClientesDrilldown(
  filtro: FaturasPorClienteFiltro,
  limite: 5 | 10 | 15,
  metrica: FaturasPorClienteMetrica,
  nivel: FaturasPorClienteDrilldownNivel,
  cliente: string | null,
  cnpj: string | null,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'top-clientes', 'drilldown', filtro, limite, metrica, nivel, cliente, cnpj],
    queryFn: () => buscarFaturasPorClienteTopClientesDrilldown(filtro, limite, metrica, nivel, cliente, cnpj),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteStatusProcesso(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'status-processo', filtro],
    queryFn: () => buscarFaturasPorClienteStatusProcesso(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteStatusEvolucao(filtro: FaturasPorClienteFiltro, granularidade: FaturasPorClienteGranularidade) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'status-processo', 'evolucao', filtro, granularidade],
    queryFn: () => buscarFaturasPorClienteStatusEvolucao(filtro, granularidade),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTabela(filtro: FaturasPorClienteFiltro, limite = 100) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'tabela', filtro, limite],
    queryFn: () => buscarFaturasPorClienteTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTabelaTotal(filtro: FaturasPorClienteFiltro) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'tabela-total', filtro],
    queryFn: () => buscarFaturasPorClienteTabelaTotal(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useFaturasPorClienteTabelaPaginada(
  filtro: FaturasPorClienteFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
) {
  return useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: ['faturas-por-cliente', 'tabela-paginada', filtro, pagina, tamanhoPagina, filtrosTabela],
    queryFn: () => buscarFaturasPorClienteTabelaPaginada(filtro, pagina, tamanhoPagina, filtrosTabela),
    placeholderData: (previousData) => previousData,
    staleTime: STALE_TIME,
    retry: 1,
  });
}
