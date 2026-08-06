import clienteAxios from '../clienteAxios';
import { baixarCsvComParametros } from '../downloadCsv';

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
  destinos?: string[],
): Promise<QuarentenaErrosPage> {
  const params = new URLSearchParams();
  params.set('pagina', String(Math.max(0, pagina - 1)));
  params.set('tamanho', String(Math.max(1, Math.min(tamanhoPagina, 500))));
  destinos?.forEach((destino) => params.append('destino', destino));

  const { data } = await clienteAxios.get<QuarentenaErrosPage>('/api/etl/quarentena/erros', { params });
  return data;
}

export async function exportarErrosQuarentenaCsv(destinos?: string[]): Promise<void> {
  const params = new URLSearchParams();
  destinos?.forEach((destino) => params.append('destino', destino));
  await baixarCsvComParametros('/api/etl/quarentena/erros/exportacao', params, 'quarentena-integracoes');
}
