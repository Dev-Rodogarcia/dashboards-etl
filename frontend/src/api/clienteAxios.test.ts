import { describe, expect, it, vi } from 'vitest';
import { tratarErroRespostaApi, type RetryableRequestConfig } from './clienteAxios';
import { SessaoExpiradaError } from '../utils/authSession';

function criarErro401(config?: RetryableRequestConfig) {
  return {
    response: { status: 401 },
    config,
  };
}

describe('tratarErroRespostaApi', () => {
  it('desloga imediatamente quando um endpoint protegido retorna 401', async () => {
    const limparSessao = vi.fn();
    const revogarSessaoRemota = vi.fn().mockResolvedValue(undefined);
    const redirecionar = vi.fn();

    await expect(tratarErroRespostaApi(criarErro401({
      url: '/api/painel/coletas',
      headers: {},
    } as RetryableRequestConfig), {
      limparSessao,
      revogarSessaoRemota,
      obterPathAtual: () => '/coletas',
      redirecionar,
    })).rejects.toBeInstanceOf(SessaoExpiradaError);

    expect(limparSessao).toHaveBeenCalledTimes(1);
    expect(revogarSessaoRemota).toHaveBeenCalledTimes(1);
    expect(redirecionar).toHaveBeenCalledWith('/login');
  });

  it('nao redireciona quando o 401 vem do login', async () => {
    const limparSessao = vi.fn();
    const revogarSessaoRemota = vi.fn();
    const redirecionar = vi.fn();
    const erro = criarErro401({
      url: '/api/auth/login',
      headers: {},
    } as RetryableRequestConfig);

    await expect(tratarErroRespostaApi(erro, {
      limparSessao,
      revogarSessaoRemota,
      obterPathAtual: () => '/login',
      redirecionar,
    })).rejects.toBe(erro);

    expect(limparSessao).not.toHaveBeenCalled();
    expect(revogarSessaoRemota).not.toHaveBeenCalled();
    expect(redirecionar).not.toHaveBeenCalled();
  });

  it('redireciona 403 para acesso negado sem limpar sessao', async () => {
    const limparSessao = vi.fn();
    const redirecionar = vi.fn();
    const erro = {
      response: { status: 403 },
      config: {
        url: '/api/painel/coletas',
        headers: {},
      } as RetryableRequestConfig,
    };

    await expect(tratarErroRespostaApi(erro, {
      limparSessao,
      obterPathAtual: () => '/coletas',
      redirecionar,
    })).rejects.toBe(erro);

    expect(limparSessao).not.toHaveBeenCalled();
    expect(redirecionar).toHaveBeenCalledWith('/acesso-negado');
  });
});
