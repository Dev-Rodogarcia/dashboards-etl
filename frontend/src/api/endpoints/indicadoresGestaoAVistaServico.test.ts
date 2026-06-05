import { beforeEach, describe, expect, it, vi } from 'vitest';
import clienteAxios from '../clienteAxios';
import {
  atualizarKpiGoalsFilial,
  atualizarKpiGoalsGlobais,
  buscarKpiGoalOverrides,
  buscarKpiGoalsEfetivos,
  buscarKpiGoalsHistorico,
  buscarKpiGoalsHistoricoPaginado,
  buscarUtilizacaoColetoresRanking,
  removerKpiGoalsOverride,
} from './indicadoresGestaoAVistaServico';
import type { KpiGoalsMap } from '../../types/indicadoresGestaoAVista';

vi.mock('../clienteAxios', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const clienteMock = clienteAxios as unknown as {
  get: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

const goals: KpiGoalsMap = {
  delivery_performance: 95,
  collector_usage: 90,
  cargo_cubage: 85,
  cargo_indemnity: 2,
  cutoff_time: 98,
};
const competencia = '2026-05-01';

describe('indicadoresGestaoAVistaServico kpi goals', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clienteMock.get.mockResolvedValue({ data: {} });
    clienteMock.put.mockResolvedValue({ data: {} });
    clienteMock.delete.mockResolvedValue({ data: {} });
  });

  it('busca metas efetivas pelo endpoint /effective', async () => {
    await buscarKpiGoalsEfetivos('GLOBAL', competencia);

    expect(clienteMock.get).toHaveBeenCalledWith('/api/kpi-goals/effective', {
      params: { branchId: 'GLOBAL', competencia },
    });
  });

  it('envia update global sem sobrescrever metas especificas', async () => {
    await atualizarKpiGoalsGlobais({ goals, competencia });

    expect(clienteMock.put).toHaveBeenCalledWith('/api/kpi-goals/global', { goals, competencia }, {
      params: { competencia },
    });
  });

  it('envia update de filial por path', async () => {
    await atualizarKpiGoalsFilial('SPO', { goals, competencia });

    expect(clienteMock.put).toHaveBeenCalledWith('/api/kpi-goals/branch/SPO', { goals, competencia }, {
      params: { competencia },
    });
  });

  it('remove override de filial por DELETE', async () => {
    await removerKpiGoalsOverride('SPO', competencia);

    expect(clienteMock.delete).toHaveBeenCalledWith('/api/kpi-goals/branch/SPO', {
      params: { competencia },
    });
  });

  it('busca historico limitado por filial', async () => {
    await buscarKpiGoalsHistorico('SPO', 10);

    expect(clienteMock.get).toHaveBeenCalledWith('/api/kpi-goals/history', {
      params: { branchId: 'SPO', limit: 10 },
    });
  });

  it('busca historico paginado por filial', async () => {
    await buscarKpiGoalsHistoricoPaginado('SPO', 2, 10);

    expect(clienteMock.get).toHaveBeenCalledWith('/api/kpi-goals/history/page', {
      params: { branchId: 'SPO', pagina: 2, tamanhoPagina: 10 },
    });
  });

  it('busca overrides por indicador', async () => {
    await buscarKpiGoalOverrides('collector_usage', competencia);

    expect(clienteMock.get).toHaveBeenCalledWith('/api/kpi-goals/overrides', {
      params: { indicatorKey: 'collector_usage', competencia },
    });
  });

  it('busca ranking de utilizacao dos coletores', async () => {
    await buscarUtilizacaoColetoresRanking({ dataInicio: '2026-05-01', dataFim: '2026-05-12', filiais: ['SPO'] });

    expect(clienteMock.get).toHaveBeenCalledWith('/api/painel/indicadores-gestao-a-vista/utilizacao-coletores/ranking', {
      params: expect.any(URLSearchParams),
    });
  });
});
