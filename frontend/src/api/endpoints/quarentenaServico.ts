import clienteAxios from '../clienteAxios';

export interface QuarentenaErroManual {
  id: number;
  destino: 'VEDACIT' | 'PPG' | string;
  chaveNfe: string | null;
  numeroNf: number | string | null;
  tentativas: number | null;
  erroLimpo: string | null;
  dataUltimaTentativa: string | null;
}

export interface QuarentenaErrosPage {
  content: QuarentenaErroManual[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export async function buscarErrosQuarentena(
  pagina: number,
  tamanhoPagina: number,
): Promise<QuarentenaErrosPage> {
  const params = new URLSearchParams();
  params.set('pagina', String(Math.max(0, pagina - 1)));
  params.set('tamanho', String(Math.max(1, Math.min(tamanhoPagina, 500))));

  const { data } = await clienteAxios.get<QuarentenaErrosPage>('/api/etl/quarentena/erros', { params });
  return data;
}
