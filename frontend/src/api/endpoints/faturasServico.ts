import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  FaturasAgingBucket,
  FaturasClienteTop,
  FaturasFiltro,
  FaturasMensalTrend,
  FaturasOverview,
  FaturasStatusProcesso,
  FaturaReconciliacaoRow,
  FaturaResumoRow,
} from '../../types/faturas';

export async function buscarFaturasOverview(filtro: FaturasFiltro): Promise<FaturasOverview> {
  const { data } = await clienteAxios.get<FaturasOverview>('/api/painel/faturas', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasMensal(filtro: FaturasFiltro): Promise<FaturasMensalTrend[]> {
  const { data } = await clienteAxios.get<FaturasMensalTrend[]>('/api/painel/faturas/mensal', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasAging(filtro: FaturasFiltro): Promise<FaturasAgingBucket[]> {
  const { data } = await clienteAxios.get<FaturasAgingBucket[]>('/api/painel/faturas/aging', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasTopClientes(
  filtro: FaturasFiltro,
  limite = 10
): Promise<FaturasClienteTop[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturasClienteTop[]>('/api/painel/faturas/top-clientes', { params });
  return data;
}

export async function buscarFaturasStatusProcesso(filtro: FaturasFiltro): Promise<FaturasStatusProcesso[]> {
  const { data } = await clienteAxios.get<FaturasStatusProcesso[]>('/api/painel/faturas/status-processo', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasReconciliacao(
  filtro: FaturasFiltro,
  limite = 50
): Promise<FaturaReconciliacaoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturaReconciliacaoRow[]>('/api/painel/faturas/reconciliacao', { params });
  return data;
}

export async function buscarFaturasTabela(
  filtro: FaturasFiltro,
  limite = 100
): Promise<FaturaResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturaResumoRow[]>('/api/painel/faturas/tabela', { params });
  return data;
}

export async function buscarFaturasTabelaTotal(filtro: FaturasFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/faturas/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarFaturasTabelaPaginada(
  filtro: FaturasFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<FaturaResumoRow>> {
  return buscarTabelaPaginada('/api/painel/faturas/tabela/paginada', filtro, pagina, tamanhoPagina);
}

export async function exportarFaturasProcessosCsv(filtro: FaturasFiltro): Promise<void> {
  await baixarCsv('/api/painel/faturas/exportacao', filtro, 'faturas-processos');
}

export async function exportarFaturasFinanceirasCsv(filtro: FaturasFiltro): Promise<void> {
  await baixarCsv('/api/painel/faturas/exportacao-financeira', filtro, 'faturas-titulos-financeiros');
}
