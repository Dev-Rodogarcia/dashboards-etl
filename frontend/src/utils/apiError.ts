import { AxiosError } from 'axios';
import { API_UNAVAILABLE_MESSAGE } from '../config/api';
import { SessaoExpiradaError, SessaoTemporariamenteIndisponivelError } from './authSession';

type RespostaErroBackend = {
  // Formato do ManipuladorGlobalExcecoes (RespostaErroPadrao)
  mensagem?: unknown;
  // Formato padrão do Spring Boot (BasicErrorController)
  message?: unknown;
  erro?: unknown;
  status?: unknown;
  codigo?: unknown;
};

export const SERVER_INSTABILITY_MESSAGE = 'Instabilidade no servidor. Tente novamente em alguns instantes.';
export const DATABASE_TIMEOUT_MESSAGE = 'Timeout na base de dados. Reduza o período ou tente novamente.';

function errorCodeIndicaTimeout(error: AxiosError): boolean {
  return error.code === 'ECONNABORTED' || error.message.toLowerCase().includes('timeout');
}

function formatarRetryAfter(segundos: number): string {
  if (!Number.isFinite(segundos) || segundos <= 0) {
    return 'alguns instantes';
  }

  if (segundos < 60) {
    return `${segundos} segundo${segundos === 1 ? '' : 's'}`;
  }

  const minutos = Math.ceil(segundos / 60);
  return `${minutos} minuto${minutos === 1 ? '' : 's'}`;
}

export function getApiErrorMessage(error: unknown, fallback = 'Não foi possível concluir a operação.'): string {
  if (error instanceof SessaoTemporariamenteIndisponivelError) {
    return API_UNAVAILABLE_MESSAGE;
  }

  if (error instanceof SessaoExpiradaError) {
    return error.message;
  }

  if (error instanceof AxiosError) {
    const data = error.response?.data as RespostaErroBackend | undefined;
    const status = error.response?.status;
    const retryAfterHeader = error.response?.headers?.['retry-after'];
    const retryAfterSeconds = Number(retryAfterHeader);

    if (status === 429) {
      return Number.isFinite(retryAfterSeconds) && retryAfterSeconds > 0
        ? `Muitas requisições em pouco tempo. Aguarde ${formatarRetryAfter(retryAfterSeconds)} e tente novamente.`
        : 'Muitas requisições em pouco tempo. Aguarde alguns instantes e tente novamente.';
    }

    if (
      status === 503
      && data?.codigo === 'ESL_CONFIGURATION_REQUIRED'
      && typeof data.mensagem === 'string'
      && data.mensagem.trim()
    ) {
      return data.mensagem;
    }

    if (status === 503) {
      return SERVER_INSTABILITY_MESSAGE;
    }

    if (status === 408 || status === 504 || errorCodeIndicaTimeout(error)) {
      return DATABASE_TIMEOUT_MESSAGE;
    }

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
 * - 'indisponivel' → sem resposta / 502 / 503 (API offline ou instavel)
 * - 'erro'      → demais erros
 */
export type TipoErro = 'periodo' | 'timeout' | 'indisponivel' | 'erro';

export function getTipoErro(error: unknown): TipoErro {
  if (error instanceof SessaoTemporariamenteIndisponivelError) {
    return 'indisponivel';
  }

  if (error instanceof AxiosError) {
    if (errorCodeIndicaTimeout(error)) return 'timeout';
    if (!error.response || error.code === 'ERR_NETWORK') return 'indisponivel';
    const status = error.response.status;
    if (status === 400) return 'periodo';
    if (status === 408 || status === 504) return 'timeout';
    if (status === 502 || status === 503) return 'indisponivel';
  }
  return 'erro';
}
