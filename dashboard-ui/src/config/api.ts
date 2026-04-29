export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:5010';

export const API_UNAVAILABLE_MESSAGE = `API indisponível em ${API_BASE_URL}. Verifique se o backend foi iniciado.`;
export const AUTH_REQUEST_TIMEOUT_MS = Number(import.meta.env.VITE_AUTH_REQUEST_TIMEOUT_MS ?? 15000);
