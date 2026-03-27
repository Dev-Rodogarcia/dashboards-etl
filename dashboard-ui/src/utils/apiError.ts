import { AxiosError } from 'axios';
import { API_UNAVAILABLE_MESSAGE } from '../config/api';
import { SessaoExpiradaError, SessaoTemporariamenteIndisponivelError } from './authSession';

type RespostaErroBackend = {
  // Formato do ManipuladorGlobalExcecoes (RespostaErroPadrao)
  mensagem?: unknown;
  // Formato padrão do Spring Boot (BasicErrorController)
  message?: unknown;
};

export function getApiErrorMessage(error: unknown, fallback = 'Não foi possível concluir a operação.'): string {
  if (error instanceof SessaoTemporariamenteIndisponivelError) {
    return API_UNAVAILABLE_MESSAGE;
  }

  if (error instanceof SessaoExpiradaError) {
    return error.message;
  }

  if (error instanceof AxiosError) {
    const data = error.response?.data as RespostaErroBackend | undefined;

    // Tenta o campo "mensagem" (formato do nosso ManipuladorGlobalExcecoes)
    if (typeof data?.mensagem === 'string' && data.mensagem.trim()) {
      return data.mensagem;
    }

    // Fallback para o campo "message" (formato padrão do Spring Boot)
    if (typeof data?.message === 'string' && data.message.trim()) {
      return data.message;
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

/**
 * Retorna o tipo de erro com base no HTTP status para uso em variantes de UI.
 * - 'periodo'   → 400 com mensagem de período (validação do backend)
 * - 'timeout'   → 408 / 504 (query demorou demais)
 * - 'indisponivel' → sem resposta (API offline)
 * - 'erro'      → demais erros
 */
export type TipoErro = 'periodo' | 'timeout' | 'indisponivel' | 'erro';

export function getTipoErro(error: unknown): TipoErro {
  if (error instanceof SessaoTemporariamenteIndisponivelError) {
    return 'indisponivel';
  }

  if (error instanceof AxiosError) {
    if (!error.response || error.code === 'ERR_NETWORK') return 'indisponivel';
    const status = error.response.status;
    if (status === 400) return 'periodo';
    if (status === 408 || status === 504) return 'timeout';
  }
  return 'erro';
}
