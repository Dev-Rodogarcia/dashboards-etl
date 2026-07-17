import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import {
  atualizarColetaEsl,
  cancelarColetaEsl,
  criarColetaEsl,
  criarCotacaoEsl,
  listarColetasEsl,
  validarNfEsl,
} from './eslService';

vi.mock('../clienteAxios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

const clienteMock = clienteAxios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
};

describe('eslService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clienteMock.get.mockResolvedValue({ data: {} });
    clienteMock.post.mockResolvedValue({ data: {} });
    clienteMock.patch.mockResolvedValue({ data: {} });
  });

  it('envia a filial em todas as operações ESL', async () => {
    await listarColetasEsl('2026-07-16', 'SPO');
    await validarNfEsl({ filial: 'SPO', chaveOrNumero: '42' });
    await criarCotacaoEsl({ filial: 'SPO', solicitacao: {} as never });
    await criarColetaEsl({ filial: 'SPO', solicitacao: {} as never });
    await atualizarColetaEsl({ filial: 'SPO', eslId: '123', solicitacao: {} });
    await cancelarColetaEsl({ filial: 'SPO', eslId: '123', solicitacao: { motivo: 'DUPLICIDADE' } });

    expect(clienteMock.get).toHaveBeenCalledWith('/api/esl/coletas', {
      params: { dataSolicitacao: '2026-07-16', filial: 'SPO' },
    });
    expect(clienteMock.get).toHaveBeenCalledWith('/api/esl/coletas/validar-nf/42', {
      params: { filial: 'SPO' },
    });
    expect(clienteMock.post).toHaveBeenCalledWith('/api/esl/cotacoes', {}, { params: { filial: 'SPO' } });
    expect(clienteMock.post).toHaveBeenCalledWith('/api/esl/coletas', {}, { params: { filial: 'SPO' } });
    expect(clienteMock.patch).toHaveBeenCalledWith('/api/esl/coletas/123', {}, { params: { filial: 'SPO' } });
    expect(clienteMock.post).toHaveBeenCalledWith('/api/esl/coletas/123/cancelamento', { motivo: 'DUPLICIDADE' }, {
      params: { filial: 'SPO' },
    });
  });
});
