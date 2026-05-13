const API_LOCAL_DEV_BASE_URL = 'http://127.0.0.1:5011';

function normalizarBaseUrl(baseUrl: string): string {
  return baseUrl.trim().replace(/\/+$/, '');
}

function resolverApiBaseUrl(): string {
  const envBaseUrl = normalizarBaseUrl(String(import.meta.env.VITE_API_BASE_URL ?? ''));
  if (envBaseUrl) {
    return envBaseUrl;
  }

  if (import.meta.env.DEV) {
    return API_LOCAL_DEV_BASE_URL;
  }

  throw new Error('VITE_API_BASE_URL é obrigatória para builds de produção.');
}

export const API_BASE_URL = resolverApiBaseUrl();
export const API_UNAVAILABLE_MESSAGE = `API indisponível em ${API_BASE_URL}. Verifique se o backend foi iniciado.`;
export const API_REQUEST_TIMEOUT_MS = Number(import.meta.env.VITE_API_REQUEST_TIMEOUT_MS ?? 90000);
export const API_DOWNLOAD_TIMEOUT_MS = Number(import.meta.env.VITE_API_DOWNLOAD_TIMEOUT_MS ?? 120000);
export const AUTH_REQUEST_TIMEOUT_MS = Number(import.meta.env.VITE_AUTH_REQUEST_TIMEOUT_MS ?? 15000);
export const HOME_COMUNICADOS_API_ENABLED = import.meta.env.VITE_HOME_COMUNICADOS_API_ENABLED === 'true';
