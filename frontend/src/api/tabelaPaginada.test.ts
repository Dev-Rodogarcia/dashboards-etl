import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from './clienteAxios';
import { buscarTabelaPaginada, normalizarPaginacaoResponse } from './tabelaPaginada';

vi.mock('./clienteAxios', () => ({
  default: {
    get: vi.fn(),
  },
}));

const clienteMock = clienteAxios as unknown as {
  get: ReturnType<typeof vi.fn>;
};

describe('tabelaPaginada', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('normaliza Page do Spring para o contrato de tabela do frontend', async () => {
    clienteMock.get.mockResolvedValue({
      data: {
        content: [{ id: 1 }],
        totalElements: 21,
        totalPages: 5,
        number: 2,
        size: 5,
      },
    });

    const resultado = await buscarTabelaPaginada('/api/tabela', {
      dataInicio: '2026-05-01',
      dataFim: '2026-05-31',
    }, 3, 5);

    expect(resultado).toEqual({
      conteudo: [{ id: 1 }],
      totalElementos: 21,
      totalPaginas: 5,
      paginaAtual: 3,
      tamanhoPagina: 5,
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('3');
    expect(params.get('tamanhoPagina')).toBe('5');
    expect(params.get('page')).toBe('2');
    expect(params.get('size')).toBe('5');
  });

  it('envia 50 linhas nos dois contratos para a tabela de manifestos', async () => {
    clienteMock.get.mockResolvedValue({
      data: {
        conteudo: Array.from({ length: 50 }, (_, index) => ({ id: index + 1 })),
        totalElementos: 120,
        totalPaginas: 3,
        paginaAtual: 2,
        tamanhoPagina: 50,
      },
    });

    const resultado = await buscarTabelaPaginada('/api/painel/manifestos/tabela/paginada', {
      dataInicio: '2026-06-01',
      dataFim: '2026-06-12',
    }, 2, 50);

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('2');
    expect(params.get('tamanhoPagina')).toBe('50');
    expect(params.get('page')).toBe('1');
    expect(params.get('size')).toBe('50');
    expect(resultado.conteudo).toHaveLength(50);
    expect(resultado.tamanhoPagina).toBe(50);
  });

  it('mantem o DTO paginado antigo quando o backend ainda usa campos em portugues', () => {
    expect(normalizarPaginacaoResponse({
      conteudo: [{ id: 7 }],
      totalElementos: 7,
      totalPaginas: 1,
      paginaAtual: 1,
      tamanhoPagina: 10,
    }, 1, 10)).toEqual({
      conteudo: [{ id: 7 }],
      totalElementos: 7,
      totalPaginas: 1,
      paginaAtual: 1,
      tamanhoPagina: 10,
    });
  });
});
