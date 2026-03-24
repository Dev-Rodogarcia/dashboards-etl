import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { ExecutivoOverview, ExecutivoTrendPoint } from '../../types/executivo';
import type { FiltroQuery } from '../../types/common';

export async function buscarExecutivoOverview(filtro: FiltroQuery): Promise<ExecutivoOverview> {
  const { data } = await clienteAxios.get<ExecutivoOverview>('/api/painel/executivo', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarExecutivoSerie(filtro: FiltroQuery): Promise<ExecutivoTrendPoint[]> {
  const { data } = await clienteAxios.get<ExecutivoTrendPoint[]>('/api/painel/executivo/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}
