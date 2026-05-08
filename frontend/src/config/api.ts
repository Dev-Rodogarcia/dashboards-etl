const API_PUBLICA_BASE_URL = 'https://api-analytics.rodogarcia.com.br';
const HOSTS_LOCAIS = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]']);

function estaEmHostPublico(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }

  return !HOSTS_LOCAIS.has(window.location.hostname);
}

export const API_BASE_URL = estaEmHostPublico()
  ? API_PUBLICA_BASE_URL
  : (import.meta.env.VITE_API_BASE_URL ?? API_PUBLICA_BASE_URL);

export const API_UNAVAILABLE_MESSAGE = `API indisponível em ${API_BASE_URL}. Verifique se o backend foi iniciado.`;
export const AUTH_REQUEST_TIMEOUT_MS = Number(import.meta.env.VITE_AUTH_REQUEST_TIMEOUT_MS ?? 15000);
export const HOME_COMUNICADOS_API_ENABLED = import.meta.env.VITE_HOME_COMUNICADOS_API_ENABLED === 'true';
