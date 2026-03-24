import { AxiosError } from 'axios';
import { API_UNAVAILABLE_MESSAGE } from '../config/api';

export function getApiErrorMessage(error: unknown, fallback = 'Não foi possível concluir a operação.'): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { mensagem?: unknown } | undefined;

    if (typeof data?.mensagem === 'string' && data.mensagem.trim()) {
      return data.mensagem;
    }

    if (!error.response || error.code === 'ERR_NETWORK') {
      return API_UNAVAILABLE_MESSAGE;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
