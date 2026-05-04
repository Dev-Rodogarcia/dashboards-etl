import clienteAxios from '../clienteAxios';
import { baixarExcel } from '../downloadExcel';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { EtlExecucaoRow, EtlExecucaoTrendPoint, EtlSaudeCharts, EtlSaudeOverview } from '../../types/etlSaude';
import type { FiltroQuery, PaginacaoResponse } from '../../types/common';

export async function buscarEtlSaudeOverview(filtro: FiltroQuery): Promise<EtlSaudeOverview> {
  const { data } = await clienteAxios.get<EtlSaudeOverview>('/api/painel/etl-saude', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarEtlSaudeSerie(filtro: FiltroQuery): Promise<EtlExecucaoTrendPoint[]> {
  const { data } = await clienteAxios.get<EtlExecucaoTrendPoint[]>('/api/painel/etl-saude/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarEtlSaudeGraficos(filtro: FiltroQuery): Promise<EtlSaudeCharts> {
  const { data } = await clienteAxios.get<EtlSaudeCharts>('/api/painel/etl-saude/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarEtlSaudeTabela(
  filtro: FiltroQuery,
  limite = 100
): Promise<EtlExecucaoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<EtlExecucaoRow[]>('/api/painel/etl-saude/tabela', { params });
  return data;
}

export async function buscarEtlSaudeTabelaTotal(filtro: FiltroQuery): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/etl-saude/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarEtlSaudeTabelaPaginada(
  filtro: FiltroQuery,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<EtlExecucaoRow>> {
  return buscarTabelaPaginada('/api/painel/etl-saude/tabela/paginada', filtro, pagina, tamanhoPagina);
}

export async function exportarEtlSaudeExcel(filtro: FiltroQuery): Promise<void> {
  await baixarExcel('/api/painel/etl-saude/exportacao', filtro, 'etl-saude');
}
