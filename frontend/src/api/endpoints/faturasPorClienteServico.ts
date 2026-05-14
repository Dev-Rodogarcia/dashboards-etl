import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  FaturaPorClienteResumoRow,
  FaturasPorClienteAgingBucket,
  FaturasPorClienteFiltro,
  FaturasPorClienteMensalTrend,
  FaturasPorClienteOverview,
  FaturasPorClienteStatusProcesso,
  FaturasPorClienteTopCliente,
} from '../../types/faturasPorCliente';
import type { TableApiFilters } from '../../types/tableFilters';

export async function buscarFaturasPorClienteOverview(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteOverview> {
  const { data } = await clienteAxios.get<FaturasPorClienteOverview>('/api/painel/faturas-por-cliente', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteMensal(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteMensalTrend[]> {
  const { data } = await clienteAxios.get<FaturasPorClienteMensalTrend[]>('/api/painel/faturas-por-cliente/mensal', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteAging(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteAgingBucket[]> {
  const { data } = await clienteAxios.get<FaturasPorClienteAgingBucket[]>('/api/painel/faturas-por-cliente/aging', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteTopClientes(
  filtro: FaturasPorClienteFiltro,
  limite = 10
): Promise<FaturasPorClienteTopCliente[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturasPorClienteTopCliente[]>('/api/painel/faturas-por-cliente/top-clientes', {
    params,
  });
  return data;
}

export async function buscarFaturasPorClienteStatusProcesso(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteStatusProcesso[]> {
  const { data } = await clienteAxios.get<FaturasPorClienteStatusProcesso[]>('/api/painel/faturas-por-cliente/status-processo', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteTabela(
  filtro: FaturasPorClienteFiltro,
  limite = 100
): Promise<FaturaPorClienteResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturaPorClienteResumoRow[]>('/api/painel/faturas-por-cliente/tabela', {
    params,
  });
  return data;
}

export async function buscarFaturasPorClienteTabelaTotal(filtro: FaturasPorClienteFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/faturas-por-cliente/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarFaturasPorClienteTabelaPaginada(
  filtro: FaturasPorClienteFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<FaturaPorClienteResumoRow>> {
  return buscarTabelaPaginada('/api/painel/faturas-por-cliente/tabela/paginada', filtro, pagina, tamanhoPagina, filtrosTabela);
}

export async function exportarFaturasPorClienteCsv(filtro: FaturasPorClienteFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await baixarCsv('/api/painel/faturas-por-cliente/exportacao', filtro, 'faturas-por-cliente', filtrosTabela);
}
