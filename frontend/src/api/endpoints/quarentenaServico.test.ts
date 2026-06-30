import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import { buscarErrosQuarentena } from './quarentenaServico';

vi.mock('../clienteAxios', () => ({
  default: {
    get: vi.fn(),
  },
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
  });

  it('normaliza limites antes de enviar ao backend', async () => {
    await buscarErrosQuarentena(-3, 900);

    const params = clienteMock.get.mock.calls[0][1].params as URLSearchParams;
    expect(params.get('pagina')).toBe('0');
    expect(params.get('tamanho')).toBe('500');
  });
});
