import clienteAxios from './clienteAxios';
import { montarQueryParams } from './endpoints/queryParams';
import { aplicarFiltrosTabelaParams } from './tableFilters';
import type { FiltroBase, PaginacaoResponse } from '../types/common';
import type { TableApiFilters } from '../types/tableFilters';

export async function buscarTabelaPaginada<T, F extends FiltroBase>(
  url: string,
  filtro: F,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<T>> {
  const params = montarQueryParams(filtro);
  aplicarFiltrosTabelaParams(params, filtrosTabela);
  params.set('pagina', String(pagina));
  params.set('tamanhoPagina', String(tamanhoPagina));

  const { data } = await clienteAxios.get<PaginacaoResponse<T>>(url, { params });
  return data;
}
