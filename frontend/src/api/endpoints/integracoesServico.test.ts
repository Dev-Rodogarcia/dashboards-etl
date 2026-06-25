import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import { buscarIntegracoesAuditoria, normalizarImagemCanhotoSrc } from './integracoesServico';

vi.mock('../clienteAxios', () => ({
  default: {
    get: vi.fn(),
  },
}));

const clienteMock = clienteAxios as unknown as {
  get: ReturnType<typeof vi.fn>;
};

describe('integracoesServico', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clienteMock.get.mockResolvedValue({
      data: {
        geradoEm: '2026-06-24T00:00:00',
        metricasConsolidadas: [],
        pendencias: {
          itens: [],
          paginacao: {
            pagina: 0,
            tamanho: 20,
            totalElementos: 0,
            totalPaginas: 0,
            primeiraPagina: true,
            ultimaPagina: true,
          },
        },
      },
    });
  });

  it('envia filtros analiticos para a auditoria de integracoes', async () => {
    await buscarIntegracoesAuditoria(2, 20, {
      tabelaBusca: 'PPG',
      tabelaCodigo: '123',
      tabelaStatus: ['ERRO_DESTINO', 'PENDENTE_FOTO'],
      tabelaColuna: {
        numeroNf: '456',
        statusCanhoto: ['PENDENTE_FOTO'],
      },
    }, 'numeroNf', 'desc');

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/integracoes', {
      params: expect.any(URLSearchParams),
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('1');
    expect(params.get('tamanho')).toBe('20');
    expect(params.get('sortField')).toBe('numeroNf');
    expect(params.get('sortDirection')).toBe('desc');
    expect(params.get('f.tabelaBusca')).toBe('PPG');
    expect(params.get('f.tabelaCodigo')).toBe('123');
    expect(params.getAll('f.tabelaStatus')).toEqual(['ERRO_DESTINO', 'PENDENTE_FOTO']);
    expect(params.get('f.tabelaColuna.numeroNf')).toBe('456');
    expect(params.getAll('f.tabelaColuna.statusCanhoto')).toEqual(['PENDENTE_FOTO']);
  });

  it('prefixa base64 puro como imagem jpeg', () => {
    expect(normalizarImagemCanhotoSrc('YWJj')).toBe('data:image/jpeg;base64,YWJj');
  });

  it('preserva data URI ja normalizada', () => {
    expect(normalizarImagemCanhotoSrc('data:image/png;base64,YWJj')).toBe('data:image/png;base64,YWJj');
  });

  it('extrai imagem de payload JSON com mime PPG', () => {
    expect(normalizarImagemCanhotoSrc({ foto: 'YWJj', mime: 'data:image/jpeg;base64' })).toBe(
      'data:image/jpeg;base64,YWJj',
    );
  });

  it('retorna null para payload vazio', () => {
    expect(normalizarImagemCanhotoSrc(null)).toBeNull();
    expect(normalizarImagemCanhotoSrc({ imagemBase64: null })).toBeNull();
  });
});
