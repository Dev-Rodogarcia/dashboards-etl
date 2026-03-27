import { AxiosError } from 'axios';

export type TipoErroSessao = 'sessao_expirada' | 'sessao_temporariamente_indisponivel';
export type AcaoBootstrapSessao = 'encerrar_sessao' | 'preservar_sessao';

class ErroSessaoBase extends Error {
  readonly tipo: TipoErroSessao;
  override cause?: unknown;

  constructor(tipo: TipoErroSessao, mensagem: string, cause?: unknown) {
    super(mensagem);
    this.name = tipo === 'sessao_expirada' ? 'SessaoExpiradaError' : 'SessaoTemporariamenteIndisponivelError';
    this.tipo = tipo;
    this.cause = cause;
  }
}

export class SessaoExpiradaError extends ErroSessaoBase {
  constructor(cause?: unknown) {
    super('sessao_expirada', 'Sessao expirada.', cause);
  }
}

export class SessaoTemporariamenteIndisponivelError extends ErroSessaoBase {
  constructor(cause?: unknown) {
    super('sessao_temporariamente_indisponivel', 'Sessao temporariamente indisponivel.', cause);
  }
}

export function ehSessaoExpiradaError(error: unknown): error is SessaoExpiradaError {
  return error instanceof SessaoExpiradaError;
}

export function ehSessaoTemporariamenteIndisponivelError(
  error: unknown,
): error is SessaoTemporariamenteIndisponivelError {
  return error instanceof SessaoTemporariamenteIndisponivelError;
}

export function classificarErroSessao(error: unknown): TipoErroSessao {
  if (ehSessaoExpiradaError(error) || ehSessaoTemporariamenteIndisponivelError(error)) {
    return error.tipo;
  }

  if (error instanceof AxiosError) {
    const status = error.response?.status;
    if (status === 401 || status === 403) {
      return 'sessao_expirada';
    }
  }

  return 'sessao_temporariamente_indisponivel';
}

export function normalizarErroSessao(error: unknown): SessaoExpiradaError | SessaoTemporariamenteIndisponivelError {
  if (ehSessaoExpiradaError(error) || ehSessaoTemporariamenteIndisponivelError(error)) {
    return error;
  }

  return classificarErroSessao(error) === 'sessao_expirada'
    ? new SessaoExpiradaError(error)
    : new SessaoTemporariamenteIndisponivelError(error);
}

export function resolverAcaoBootstrapSessao(error: unknown): AcaoBootstrapSessao {
  return classificarErroSessao(error) === 'sessao_expirada'
    ? 'encerrar_sessao'
    : 'preservar_sessao';
}
