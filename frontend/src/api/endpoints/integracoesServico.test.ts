import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import { buscarIntegracoesAuditoria, buscarIntegracoesEvolucaoDiaria } from './integracoesServico';

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
    await buscarIntegracoesAuditoria(
      2,
      20,
      '2026-06-01',
      '2026-06-24',
      {
        tabelaBusca: 'PPG',
        tabelaCodigo: '123',
        tabelaStatus: ['ERRO_DESTINO', 'PENDENTE_FOTO'],
        tabelaColuna: {
          numeroNf: '456',
          statusCanhoto: ['PENDENTE_FOTO'],
        },
      },
      'numeroNf',
      'desc',
      'SUCESSO',
    );

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/integracoes', {
      params: expect.any(URLSearchParams),
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('1');
    expect(params.get('tamanho')).toBe('20');
    expect(params.get('escopo')).toBe('SUCESSO');
    expect(params.get('dataInicial')).toBe('2026-06-01');
    expect(params.get('dataFinal')).toBe('2026-06-24');
    expect(params.get('sortField')).toBe('numeroNf');
    expect(params.get('sortDirection')).toBe('desc');
    expect(params.get('f.tabelaBusca')).toBe('PPG');
    expect(params.get('f.tabelaCodigo')).toBe('123');
    expect(params.getAll('f.tabelaStatus')).toEqual(['ERRO_DESTINO', 'PENDENTE_FOTO']);
    expect(params.get('f.tabelaColuna.numeroNf')).toBe('456');
    expect(params.getAll('f.tabelaColuna.statusCanhoto')).toEqual(['PENDENTE_FOTO']);
  });

  it('usa pendencias como escopo padrao', async () => {
    await buscarIntegracoesAuditoria(1, 10, '2026-06-01', '2026-06-24');

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('escopo')).toBe('PENDENCIAS');
  });

  it('consulta evolucao diaria com periodo global', async () => {
    await buscarIntegracoesEvolucaoDiaria('2026-06-01', '2026-06-24', 'SUCESSO');

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/integracoes/evolucao-diaria', {
      params: expect.any(URLSearchParams),
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('dataInicial')).toBe('2026-06-01');
    expect(params.get('dataFinal')).toBe('2026-06-24');
    expect(params.get('escopo')).toBe('SUCESSO');
  });

});
