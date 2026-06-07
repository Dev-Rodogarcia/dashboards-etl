import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { EtlExecucaoRow, EtlExecucaoTrendPoint, EtlLogExtracaoAuditoriaRow, EtlSaudeCharts, EtlSaudeOverview } from '../../types/etlSaude';
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

export async function buscarEtlSaudeTabela(filtro: FiltroQuery): Promise<EtlLogExtracaoAuditoriaRow[]> {
  const params = montarQueryParams(filtro);
  const { data } = await clienteAxios.get<EtlLogExtracaoAuditoriaRow[]>('/api/painel/etl-saude/tabela', { params });
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

export async function exportarEtlSaudeCsv(filtro: FiltroQuery): Promise<void> {
  await baixarCsv('/api/painel/etl-saude/exportacao', filtro, 'etl-saude');
}
