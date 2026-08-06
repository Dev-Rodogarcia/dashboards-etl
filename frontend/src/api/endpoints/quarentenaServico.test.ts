import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import { buscarErrosQuarentena, exportarErrosQuarentenaCsv } from './quarentenaServico';
import { baixarCsvComParametros } from '../downloadCsv';

vi.mock('../clienteAxios', () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock('../downloadCsv', () => ({
  baixarCsvComParametros: vi.fn(),
}));

const clienteMock = clienteAxios as unknown as {
  get: ReturnType<typeof vi.fn>;
};

describe('quarentenaServico', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clienteMock.get.mockResolvedValue({
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 100,
        number: 0,
        first: true,
        last: true,
        numberOfElements: 0,
        empty: true,
      },
    });
  });

  it('consulta erros manuais usando paginacao zero-based da API', async () => {
    await buscarErrosQuarentena(2, 100);

    expect(clienteMock.get).toHaveBeenCalledWith('/api/etl/quarentena/erros', {
      params: expect.any(URLSearchParams),
    });

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('1');
    expect(params.get('tamanho')).toBe('100');
    expect(params.getAll('destino')).toEqual([]);
  });

  it('normaliza limites antes de enviar ao backend', async () => {
    await buscarErrosQuarentena(-3, 900, ['SELIA']);

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('0');
    expect(params.get('tamanho')).toBe('500');
    expect(params.getAll('destino')).toEqual(['SELIA']);
  });

  it('preserva SELIA na quarentena para a identificação visual da tela', async () => {
    clienteMock.get.mockResolvedValueOnce({
      data: {
        content: [{
          id: 10,
          destino: 'SELIA',
          chaveNfe: '35260800000000000000550010000000011000000010',
          numeroNf: 1,
          tentativas: 3,
          erroLimpo: 'Comprovante POD ausente.',
          dataUltimaTentativa: '2026-08-05T16:00:00',
        }],
        totalElements: 1,
        totalPages: 1,
        size: 100,
        number: 0,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false,
      },
    });

    const resposta = await buscarErrosQuarentena(1, 100);

    expect(resposta.content[0]).toMatchObject({ destino: 'SELIA', tentativas: 3 });
  });

  it('exporta os destinos selecionados da quarentena', async () => {
    await exportarErrosQuarentenaCsv(['PPG', 'VEDACIT']);

    expect(baixarCsvComParametros).toHaveBeenCalledWith(
      '/api/etl/quarentena/erros/exportacao',
      expect.any(URLSearchParams),
      'quarentena-integracoes',
    );
    const params = vi.mocked(baixarCsvComParametros).mock.calls[0][1];
    expect(params.getAll('destino')).toEqual(['PPG', 'VEDACIT']);
  });
});
