import { describe, expect, it } from 'vitest';
import { FATURAMENTO_LEGACY_API_BASE, mapFretesOverviewToFaturamento } from './faturamentoServico';
import type { FaturamentoOverview } from '../../types/faturamento';

describe('faturamentoServico', () => {
  it('mantem o endpoint legado confinado na camada de servico', () => {
    expect(FATURAMENTO_LEGACY_API_BASE).toBe('/api/painel/fretes');
  });

  it('mapeia o contrato legado para a semantica de faturamento sem mutar o payload', () => {
    const legado: FaturamentoOverview = {
      updatedAt: '2026-05-24T10:00:00',
      totalFretes: 12,
      receitaBruta: 1000,
      valorFrete: 900,
      ticketMedio: 83.33,
      pesoTaxadoTotal: 100,
      volumesTotais: 8,
      pctCteEmitido: 95,
      pctNfseEmitida: 10,
      fretesPrevisaoVencida: 2,
      metaFaturamento: 1200,
      percentualAtingimentoFaturamento: 83.33,
      faturamentoDiario: {
        totalDiasUteisMes: 20,
        diasUteisDecorridos: 10,
        diasUteisRestantes: 10,
        metaDiariaBase: 60,
        faturamentoDiarioReal: 100,
        metaDiariaDinamica: 110,
        faturamentoFaltante: 200,
        tendenciaFaturamento: 2000,
        tendenciaPercentual: 0.6,
      },
    };

    const faturamento = mapFretesOverviewToFaturamento(legado);

    expect(faturamento).toEqual(legado);
    expect(faturamento).not.toBe(legado);
  });
});
