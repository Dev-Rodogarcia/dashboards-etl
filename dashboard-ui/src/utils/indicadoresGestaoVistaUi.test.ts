import { describe, expect, it } from 'vitest';
import {
  aggregateCubagemRanking,
  aggregateHorariosRanking,
  aggregateIndenizacaoRanking,
  aggregatePerformanceRanking,
  aggregateUtilizacaoRanking,
  avaliarMetaIndicador,
  calcularProgressoMeta,
} from './indicadoresGestaoVistaUi';

describe('indicadoresGestaoVistaUi', () => {
  it('avalia meta minima corretamente', () => {
    expect(avaliarMetaIndicador({
      value: 92,
      threshold: 95,
      mode: 'atLeast',
      hasData: true,
    })).toEqual({
      tone: 'warning',
      label: 'Em atenção',
      met: false,
      delta: -3,
      progressPct: 96.8,
    });
  });

  it('avalia limite maximo corretamente', () => {
    expect(avaliarMetaIndicador({
      value: 0.18,
      threshold: 0.2,
      mode: 'atMost',
      hasData: true,
    })).toEqual({
      tone: 'positive',
      label: 'Dentro da meta',
      met: true,
      delta: -0.02,
      progressPct: 100,
    });
  });

  it('marca indicador como critico quando fica abaixo de 50% da meta', () => {
    expect(avaliarMetaIndicador({
      value: 40,
      threshold: 95,
      mode: 'atLeast',
      hasData: true,
    })).toEqual({
      tone: 'negative',
      label: 'Crítico',
      met: false,
      delta: -55,
      progressPct: 42.1,
    });
  });

  it('calcula progresso invertido para metas de teto', () => {
    expect(calcularProgressoMeta(0.3, 0.2, 'atMost')).toBe(66.7);
  });

  it('agrega performance por regiao', () => {
    const ranking = aggregatePerformanceRanking([
      { date: '2026-04-01', responsavelRegiaoDestino: 'SPO', totalEntregas: 10, entregasNoPrazo: 6, entregasSemDados: 1, pctNoPrazo: 60 },
      { date: '2026-04-02', responsavelRegiaoDestino: 'SPO', totalEntregas: 5, entregasNoPrazo: 3, entregasSemDados: 0, pctNoPrazo: 60 },
    ]);

    expect(ranking).toEqual([
      {
        group: 'SPO',
        totalEntregas: 15,
        entregasNoPrazo: 9,
        entregasSemDados: 1,
        entregasForaDoPrazo: 5,
        pctNoPrazo: 60,
      },
    ]);
  });

  it('agrega utilizacao por filial', () => {
    const ranking = aggregateUtilizacaoRanking([
      { date: '2026-04-01', filial: 'SPO', ordensConferencia: 6, manifestosEmitidos: 5, manifestosDescarregamento: 5, totalManifestos: 10, pctUtilizacao: 60 },
      { date: '2026-04-02', filial: 'SPO', ordensConferencia: 4, manifestosEmitidos: 3, manifestosDescarregamento: 2, totalManifestos: 5, pctUtilizacao: 80 },
    ]);

    expect(ranking[0]).toEqual({
      group: 'SPO',
      ordensConferencia: 10,
      manifestosEmitidos: 8,
      manifestosDescarregamento: 7,
      totalManifestos: 15,
      pctUtilizacao: 66.7,
    });
  });

  it('agrega cubagem por filial', () => {
    const ranking = aggregateCubagemRanking([
      { date: '2026-04-01', filial: 'CWB', totalFretes: 10, fretesCubados: 8, pctCubagem: 80 },
      { date: '2026-04-02', filial: 'CWB', totalFretes: 5, fretesCubados: 2, pctCubagem: 40 },
    ]);

    expect(ranking[0]).toEqual({
      group: 'CWB',
      totalFretes: 15,
      fretesCubados: 10,
      fretesNaoCubados: 5,
      pctCubagem: 66.7,
    });
  });

  it('agrega indenizacao por filial', () => {
    const ranking = aggregateIndenizacaoRanking([
      { date: '2026-04-01', filial: 'REC', totalSinistros: 1, valorIndenizadoAbs: 100, faturamentoBase: 10000, pctIndenizacao: 1 },
      { date: '2026-04-02', filial: 'REC', totalSinistros: 2, valorIndenizadoAbs: 50, faturamentoBase: 5000, pctIndenizacao: 1 },
    ]);

    expect(ranking[0]).toEqual({
      group: 'REC',
      totalSinistros: 3,
      valorIndenizadoAbs: 150,
      faturamentoBase: 15000,
      pctIndenizacao: 1,
    });
  });

  it('agrega horarios por filial', () => {
    const ranking = aggregateHorariosRanking([
      { date: '2026-04-01', filial: 'NHB', saidasNoHorario: 9, totalProgramado: 10, pctNoHorario: 90 },
      { date: '2026-04-02', filial: 'NHB', saidasNoHorario: 6, totalProgramado: 10, pctNoHorario: 60 },
    ]);

    expect(ranking[0]).toEqual({
      group: 'NHB',
      saidasNoHorario: 15,
      saidasForaDoHorario: 5,
      totalProgramado: 20,
      pctNoHorario: 75,
    });
  });
});
