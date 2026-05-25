import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import { buscarPerformanceHistorico, buscarPerformanceTabela, exportarPerformanceCsv } from './performanceServico';

vi.mock('../downloadArquivo', () => ({
  extrairNomeArquivo: vi.fn((_contentDisposition: string | undefined, fallback: string) => fallback),
  salvarBlobComoArquivo: vi.fn(),
}));

vi.mock('../clienteAxios', () => ({
  default: {
    get: vi.fn(),
  },
}));

const clienteMock = clienteAxios as unknown as {
  get: ReturnType<typeof vi.fn>;
};

describe('performanceServico', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clienteMock.get.mockResolvedValue({ data: [], headers: {} });
  });

  it('envia datas dinâmicas no histórico de performance', async () => {
    await buscarPerformanceHistorico({
      dataInicio: '2026-03-01',
      dataFim: '2026-05-25',
      filiais: ['SPO'],
      status: ['Finalizada'],
      pagadores: ['Cliente A'],
    });

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/performance/historico', {
      params: expect.any(URLSearchParams),
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('dataInicio')).toBe('2026-03-01');
    expect(params.get('dataFim')).toBe('2026-05-25');
    expect(params.getAll('f.filiais')).toEqual(['SPO']);
    expect(params.getAll('f.status')).toEqual(['Finalizada']);
    expect(params.getAll('f.pagadores')).toEqual(['Cliente A']);
  });

  it('envia page e size para a tabela de performance no endpoint Spring Page', async () => {
    await buscarPerformanceTabela({
      dataInicio: '2026-05-01',
      dataFim: '2026-05-25',
      responsaveis: ['SPO'],
      pagadores: ['Cliente B'],
    }, 3, 50, {
      tabelaBusca: 'atraso',
      tabelaStatus: ['Finalizada'],
      tabelaColuna: {
        numeroMinuta: '123',
        cidadeDestino: 'Campinas',
      },
    });

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/performance/tabela', {
      params: expect.any(URLSearchParams),
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('page')).toBe('2');
    expect(params.get('size')).toBe('50');
    expect(params.getAll('f.responsaveis')).toEqual(['SPO']);
    expect(params.getAll('f.pagadores')).toEqual(['Cliente B']);
    expect(params.get('f.tabelaBusca')).toBe('atraso');
    expect(params.getAll('f.tabelaStatus')).toEqual(['Finalizada']);
    expect(params.get('f.tabelaColuna.numeroMinuta')).toBe('123');
    expect(params.get('f.tabelaColuna.cidadeDestino')).toBe('Campinas');
  });

  it('exporta CSV de performance com filtros globais e filtros analíticos', async () => {
    clienteMock.get.mockResolvedValueOnce({
      data: new Blob(['numeroMinuta\r\n123\r\n'], { type: 'text/csv;charset=UTF-8' }),
      headers: { 'content-type': 'text/csv;charset=UTF-8' },
    });

    await exportarPerformanceCsv({
      dataInicio: '2026-05-01',
      dataFim: '2026-05-25',
      filiais: ['SPO'],
      pagadores: ['Cliente A'],
    }, {
      tabelaBusca: 'Campinas',
      tabelaStatus: ['Finalizada'],
    });

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/performance/exportacao', expect.objectContaining({
      params: expect.any(URLSearchParams),
      responseType: 'blob',
    }));

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.getAll('f.filiais')).toEqual(['SPO']);
    expect(params.getAll('f.pagadores')).toEqual(['Cliente A']);
    expect(params.get('f.tabelaBusca')).toBe('Campinas');
    expect(params.getAll('f.tabelaStatus')).toEqual(['Finalizada']);
  });
});
