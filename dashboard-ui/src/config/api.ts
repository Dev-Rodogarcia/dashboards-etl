export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const API_UNAVAILABLE_MESSAGE = `API indisponível em ${API_BASE_URL}. Verifique se o backend foi iniciado.`;
