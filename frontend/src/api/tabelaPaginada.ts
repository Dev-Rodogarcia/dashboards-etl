import clienteAxios from './clienteAxios';
import { montarQueryParams } from './endpoints/queryParams';
import type { FiltroBase, PaginacaoResponse } from '../types/common';

export async function buscarTabelaPaginada<T, F extends FiltroBase>(
  url: string,
  filtro: F,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<T>> {
  const params = montarQueryParams(filtro);
  params.set('pagina', String(pagina));
  params.set('tamanhoPagina', String(tamanhoPagina));

  const { data } = await clienteAxios.get<PaginacaoResponse<T>>(url, { params });
  return data;
}
