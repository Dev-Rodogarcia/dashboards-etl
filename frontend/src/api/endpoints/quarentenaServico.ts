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

export interface QuarentenaHistoricoRepescagem {
  id: number;
  destino: string;
  chaveNfe: string | null;
  numeroNf: number | string | null;
  etapa: string;
  entradaQuarentenaEm: string | null;
  reprocessadoEm: string | null;
  resultado: 'SUCESSO' | 'PENDENTE' | 'ERRO' | string;
  motivo: string | null;
}

export interface QuarentenaHistoricoPage {
  content: QuarentenaHistoricoRepescagem[];
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

export async function buscarHistoricoRepescagens(
  pagina: number,
  tamanhoPagina: number,
  dataInicial: string,
  dataFinal: string,
  destinos?: string[],
): Promise<QuarentenaHistoricoPage> {
  const params = new URLSearchParams();
  params.set('pagina', String(Math.max(0, pagina - 1)));
  params.set('tamanho', String(Math.max(1, Math.min(tamanhoPagina, 500))));
  params.set('dataInicial', dataInicial);
  params.set('dataFinal', dataFinal);
  destinos?.forEach((destino) => params.append('destino', destino));
  const { data } = await clienteAxios.get<QuarentenaHistoricoPage>(
    '/api/etl/quarentena/historico', { params },
  );
  return data;
}

export async function exportarHistoricoRepescagensCsv(
  dataInicial: string,
  dataFinal: string,
  destinos?: string[],
): Promise<void> {
  const params = new URLSearchParams();
  params.set('dataInicial', dataInicial);
  params.set('dataFinal', dataFinal);
  destinos?.forEach((destino) => params.append('destino', destino));
  await baixarCsvComParametros('/api/etl/quarentena/historico/exportacao', params, 'historico-repescagens-integracoes');
}
