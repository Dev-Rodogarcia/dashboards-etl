import { describe, expect, it, vi } from 'vitest';
import type { LoginResponse } from '../types/auth';
import type { PermissionMap } from '../types/access';
import { tratarErroRespostaApi, type RetryableRequestConfig } from './clienteAxios';
import { SessaoExpiradaError, SessaoTemporariamenteIndisponivelError } from '../utils/authSession';

function criarPermissoes(): PermissionMap {
  return {
    coletas: true,
    manifestos: false,
    fretes: false,
    tracking: false,
    faturas: false,
    faturasPorCliente: false,
    contasAPagar: false,
    cotacoes: false,
    indicadoresGestaoAVista: false,
    executivo: false,
    etlSaude: false,
    dimensoes: false,
  };
}

function criarRespostaLogin(token = 'token-renovado'): LoginResponse {
  return {
    token,
    exigeTrocaSenha: false,
    usuario: {
      id: '1',
      nome: 'Painel',
      email: 'painel@empresa.com',
      papel: 'usuario_comum',
      setor: { id: '10', nome: 'Operacoes' },
      permissoesEfetivas: criarPermissoes(),
      filiaisPermitidasEfetivas: ['SP'],
      exigeTrocaSenha: false,
    },
  };
}

function criarErro401(config?: RetryableRequestConfig) {
  return {
    response: { status: 401 },
    config,
  };
}

describe('tratarErroRespostaApi', () => {
  it('renova a sessao e repete a requisicao original quando o refresh tem sucesso', async () => {
    const request = {
      url: '/api/painel/coletas',
      headers: {},
    } as RetryableRequestConfig;
    const repetirRequisicao = vi.fn().mockResolvedValue({ data: 'ok' });
    const renovarSessao = vi.fn().mockResolvedValue(criarRespostaLogin());

    const resultado = await tratarErroRespostaApi(criarErro401(request), {
      renovarSessao,
      repetirRequisicao,
      limparSessao: vi.fn(),
      obterPathAtual: () => '/coletas',
      redirecionar: vi.fn(),
    });

    expect(request._retry).toBe(true);
    expect(request.headers.Authorization).toBe('Bearer token-renovado');
    expect(renovarSessao).toHaveBeenCalledTimes(1);
    expect(repetirRequisicao).toHaveBeenCalledWith(request);
    expect(resultado).toEqual({ data: 'ok' });
  });

  it('desloga quando o refresh indica sessao expirada de verdade', async () => {
    const limparSessao = vi.fn();
    const redirecionar = vi.fn();

    await expect(tratarErroRespostaApi(criarErro401({
      url: '/api/painel/coletas',
      headers: {},
    } as RetryableRequestConfig), {
      renovarSessao: vi.fn().mockRejectedValue(new SessaoExpiradaError()),
      repetirRequisicao: vi.fn(),
      limparSessao,
      obterPathAtual: () => '/coletas',
      redirecionar,
    })).rejects.toBeInstanceOf(SessaoExpiradaError);

    expect(limparSessao).toHaveBeenCalledTimes(1);
    expect(redirecionar).toHaveBeenCalledWith('/login');
  });

  it('nao desloga quando o refresh falha temporariamente por indisponibilidade', async () => {
    const limparSessao = vi.fn();
    const redirecionar = vi.fn();

    await expect(tratarErroRespostaApi(criarErro401({
      url: '/api/painel/coletas',
      headers: {},
    } as RetryableRequestConfig), {
      renovarSessao: vi.fn().mockRejectedValue(new SessaoTemporariamenteIndisponivelError()),
      repetirRequisicao: vi.fn(),
      limparSessao,
      obterPathAtual: () => '/coletas',
      redirecionar,
    })).rejects.toBeInstanceOf(SessaoTemporariamenteIndisponivelError);

    expect(limparSessao).not.toHaveBeenCalled();
    expect(redirecionar).not.toHaveBeenCalled();
  });
});
