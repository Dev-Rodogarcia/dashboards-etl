import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { EtlExecucaoRow, EtlExecucaoTrendPoint, EtlSaudeCharts, EtlSaudeOverview } from '../../types/etlSaude';
import type { FiltroQuery } from '../../types/common';

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
