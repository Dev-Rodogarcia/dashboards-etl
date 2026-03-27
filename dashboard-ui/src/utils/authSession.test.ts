import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import {
  SessaoExpiradaError,
  SessaoTemporariamenteIndisponivelError,
  normalizarErroSessao,
  resolverAcaoBootstrapSessao,
} from './authSession';

function criarAxiosError(status?: number): AxiosError {
  return new AxiosError(
    status ? `HTTP ${status}` : 'Falha de rede',
    status ? undefined : 'ERR_NETWORK',
    undefined,
    undefined,
    status ? { status } as never : undefined,
  );
}

describe('authSession', () => {
  it('normaliza 401 e 403 como sessao expirada', () => {
    expect(normalizarErroSessao(criarAxiosError(401))).toBeInstanceOf(SessaoExpiradaError);
    expect(normalizarErroSessao(criarAxiosError(403))).toBeInstanceOf(SessaoExpiradaError);
  });

  it('normaliza falhas de rede e 5xx como sessao temporariamente indisponivel', () => {
    expect(normalizarErroSessao(criarAxiosError())).toBeInstanceOf(SessaoTemporariamenteIndisponivelError);
    expect(normalizarErroSessao(criarAxiosError(503))).toBeInstanceOf(SessaoTemporariamenteIndisponivelError);
  });

  it('preserva a sessao no bootstrap quando a API esta temporariamente indisponivel', () => {
    expect(resolverAcaoBootstrapSessao(new SessaoTemporariamenteIndisponivelError())).toBe('preservar_sessao');
    expect(resolverAcaoBootstrapSessao(criarAxiosError())).toBe('preservar_sessao');
    expect(resolverAcaoBootstrapSessao(criarAxiosError(503))).toBe('preservar_sessao');
  });

  it('encerra a sessao no bootstrap quando a autenticacao realmente expirou', () => {
    expect(resolverAcaoBootstrapSessao(new SessaoExpiradaError())).toBe('encerrar_sessao');
    expect(resolverAcaoBootstrapSessao(criarAxiosError(401))).toBe('encerrar_sessao');
  });
});
