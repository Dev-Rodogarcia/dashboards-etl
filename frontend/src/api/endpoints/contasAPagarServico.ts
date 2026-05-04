import clienteAxios from '../clienteAxios';
import { baixarExcel } from '../downloadExcel';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  ContaPagarResumoRow,
  ContasAPagarCharts,
  ContasAPagarFiltro,
  ContasAPagarMensalTrend,
  ContasAPagarOverview,
} from '../../types/contasAPagar';

export async function buscarContasAPagarOverview(filtro: ContasAPagarFiltro): Promise<ContasAPagarOverview> {
  const { data } = await clienteAxios.get<ContasAPagarOverview>('/api/painel/contas-a-pagar', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarContasAPagarSerie(filtro: ContasAPagarFiltro): Promise<ContasAPagarMensalTrend[]> {
  const { data } = await clienteAxios.get<ContasAPagarMensalTrend[]>('/api/painel/contas-a-pagar/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarContasAPagarGraficos(filtro: ContasAPagarFiltro): Promise<ContasAPagarCharts> {
  const { data } = await clienteAxios.get<ContasAPagarCharts>('/api/painel/contas-a-pagar/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarContasAPagarTabela(
  filtro: ContasAPagarFiltro,
  limite = 100
): Promise<ContaPagarResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<ContaPagarResumoRow[]>('/api/painel/contas-a-pagar/tabela', { params });
  return data;
}

export async function buscarContasAPagarTabelaTotal(filtro: ContasAPagarFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/contas-a-pagar/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarContasAPagarTabelaPaginada(
  filtro: ContasAPagarFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<ContaPagarResumoRow>> {
  return buscarTabelaPaginada('/api/painel/contas-a-pagar/tabela/paginada', filtro, pagina, tamanhoPagina);
}

export async function exportarContasAPagarExcel(filtro: ContasAPagarFiltro): Promise<void> {
  await baixarExcel('/api/painel/contas-a-pagar/exportacao', filtro, 'contas-a-pagar');
}
