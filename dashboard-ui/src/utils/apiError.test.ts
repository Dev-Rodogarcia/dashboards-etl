import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { getApiErrorMessage, getTipoErro } from './apiError';

function criarAxiosError({
  status,
  mensagem,
  headers,
  code,
  message,
}: {
  status?: number;
  mensagem?: string;
  headers?: Record<string, string>;
  code?: string;
  message?: string;
} = {}): AxiosError {
  return new AxiosError(
    message ?? (status ? `HTTP ${status}` : 'Falha'),
    code,
    undefined,
    undefined,
    status ? {
      status,
      data: mensagem ? { mensagem } : undefined,
      headers: headers ?? {},
    } as never : undefined,
  );
}

describe('apiError', () => {
  it('preserva a mensagem detalhada do backend quando ela existe', () => {
    expect(getApiErrorMessage(criarAxiosError({
      status: 401,
      mensagem: 'Conta temporariamente bloqueada. Tente novamente mais tarde.',
    }))).toBe('Conta temporariamente bloqueada. Tente novamente mais tarde.');
  });

  it('traduz rate limit com Retry-After para uma mensagem orientada ao usuario', () => {
    expect(getApiErrorMessage(criarAxiosError({
      status: 429,
      headers: { 'retry-after': '90' },
    }))).toBe('Muitas tentativas de autenticação. Aguarde 2 minutos e tente novamente.');
  });

  it('traduz timeout do axios para mensagem explicita e classifica como timeout', () => {
    const error = criarAxiosError({
      code: 'ECONNABORTED',
      message: 'timeout of 15000ms exceeded',
    });

    expect(getApiErrorMessage(error)).toBe(
      'A autenticação demorou mais do que o esperado. Verifique a conexão com a API e tente novamente.',
    );
    expect(getTipoErro(error)).toBe('timeout');
  });
});
