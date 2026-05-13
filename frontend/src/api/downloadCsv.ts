import clienteAxios from './clienteAxios';
import { extrairNomeArquivo, salvarBlobComoArquivo } from './downloadArquivo';
import { montarQueryParams } from './endpoints/queryParams';
import { API_DOWNLOAD_TIMEOUT_MS } from '../config/api';

export { extrairNomeArquivo, salvarBlobComoArquivo } from './downloadArquivo';

export type FiltroCsv = { dataInicio: string; dataFim: string };

export function criarConfigDownloadCsv<T extends FiltroCsv>(filtro: T) {
  return {
    params: montarQueryParams(filtro),
    responseType: 'blob' as const,
    timeout: API_DOWNLOAD_TIMEOUT_MS,
  };
}

function normalizarHeader(header: unknown): string | undefined {
  if (Array.isArray(header)) {
    return header[0] == null ? undefined : String(header[0]);
  }
  return header == null ? undefined : String(header);
}

function contentTypeIndicaCsv(contentType: string | undefined): boolean {
  if (!contentType) {
    return true;
  }

  const valor = contentType.toLowerCase();
  return valor.includes('text/csv')
    || valor.includes('application/csv')
    || valor.includes('application/octet-stream');
}

function contentTypeIndicaErro(contentType: string | undefined): boolean {
  if (!contentType) {
    return false;
  }

  const valor = contentType.toLowerCase();
  return valor.includes('application/json')
    || valor.includes('text/html')
    || valor.includes('text/plain');
}

function extrairMensagemErroDownload(texto: string): string {
  try {
    const json = JSON.parse(texto) as { mensagem?: unknown; message?: unknown; erro?: unknown };
    if (typeof json.mensagem === 'string' && json.mensagem.trim()) {
      return json.mensagem;
    }
    if (typeof json.message === 'string' && json.message.trim()) {
      return json.message;
    }
    if (typeof json.erro === 'string' && json.erro.trim()) {
      return json.erro;
    }
  } catch {
    // A resposta tambem pode ser HTML/texto vindo de proxy ou Cloudflare.
  }

  const textoLimpo = texto
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return textoLimpo || 'A API retornou uma resposta inesperada em vez do arquivo CSV.';
}

async function validarRespostaCsv(blob: Blob, contentTypeHeader: string | undefined): Promise<void> {
  const contentType = contentTypeHeader || blob.type;
  if (contentTypeIndicaCsv(contentType)) {
    return;
  }

  if (contentTypeIndicaErro(contentType)) {
    throw new Error(extrairMensagemErroDownload(await blob.text()));
  }

  throw new Error(`A API retornou um arquivo em formato inesperado: ${contentType}.`);
}

export async function baixarCsv<T extends FiltroCsv>(endpoint: string, filtro: T, nomeFallback: string): Promise<void> {
  const response = await clienteAxios.get<Blob>(endpoint, criarConfigDownloadCsv(filtro));
  const contentDisposition = normalizarHeader(response.headers['content-disposition']);
  const contentType = normalizarHeader(response.headers['content-type']);
  const nomeArquivo = extrairNomeArquivo(contentDisposition, `${nomeFallback}.csv`);

  await validarRespostaCsv(response.data, contentType);
  salvarBlobComoArquivo(response.data, nomeArquivo);
}
