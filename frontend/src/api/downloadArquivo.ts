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
