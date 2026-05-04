import clienteAxios from './clienteAxios';
import { montarQueryParams } from './endpoints/queryParams';

export type FiltroExcel = { dataInicio: string; dataFim: string };

export function criarConfigDownloadExcel<T extends FiltroExcel>(filtro: T) {
  return {
    params: montarQueryParams(filtro),
    responseType: 'blob' as const,
  };
}

export function extrairNomeArquivo(contentDisposition: string | undefined, fallback: string): string {
  if (!contentDisposition) {
    return fallback;
  }

  const filenameStar = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  if (filenameStar?.[1]) {
    return decodeURIComponent(filenameStar[1].replace(/"/g, '').trim());
  }

  const filename = /filename="?([^";]+)"?/i.exec(contentDisposition);
  if (filename?.[1]) {
    return filename[1].trim();
  }

  return fallback;
}

export function salvarBlobComoArquivo(blob: Blob, nomeArquivo: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nomeArquivo;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export async function baixarExcel<T extends FiltroExcel>(endpoint: string, filtro: T, nomeFallback: string): Promise<void> {
  const response = await clienteAxios.get<Blob>(endpoint, criarConfigDownloadExcel(filtro));
  const contentDisposition = response.headers['content-disposition'];
  const nomeArquivo = extrairNomeArquivo(
    Array.isArray(contentDisposition) ? contentDisposition[0] : contentDisposition,
    `${nomeFallback}.xlsx`,
  );

  salvarBlobComoArquivo(response.data, nomeArquivo);
}
