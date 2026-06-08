import { afterEach, describe, expect, it, vi } from 'vitest';
import { DATE_RANGE_PRESETS, resolverPresetAtivo } from './dateRangePresets';

const DATA_FROZEN = new Date('2026-05-23T01:36:00.000Z'); // local = 2026-05-22 UTC-3
const DATA_CONFLITO_MENSAL = new Date('2026-06-08T15:00:00.000Z');

describe('DATE_RANGE_PRESETS', () => {
  afterEach(() => vi.useRealTimers());

  it('mantem os atalhos corporativos esperados', () => {
    expect(DATE_RANGE_PRESETS.map((preset) => preset.label)).toEqual([
      '7d',
      '15d',
      '30d',
      '60d',
      '90d',
      '180d',
      'Este mês',
      'Mês passado',
    ]);
  });

  it('calcula atalhos relativos no fuso local', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_FROZEN);

    expect(range('7d')).toEqual({ dataInicio: '2026-05-15', dataFim: '2026-05-22' });
    expect(range('90d')).toEqual({ dataInicio: '2026-02-21', dataFim: '2026-05-22' });
    expect(range('Este mês')).toEqual({ dataInicio: '2026-05-01', dataFim: '2026-05-22' });
    expect(range('Mês passado')).toEqual({ dataInicio: '2026-04-01', dataFim: '2026-04-30' });
  });

  it('prioriza Este mês quando o range tambem coincide com 7d', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_CONFLITO_MENSAL);

    const esteMes = range('Este mês');

    expect(range('7d')).toEqual(esteMes);
    expect(resolverPresetAtivo(esteMes.dataInicio, esteMes.dataFim)).toBe('Este mês');
  });

  it('preserva o preset clicado quando dois atalhos produzem o mesmo range', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_CONFLITO_MENSAL);

    const esteMes = range('Este mês');

    expect(resolverPresetAtivo(esteMes.dataInicio, esteMes.dataFim, '7d')).toBe('7d');
    expect(resolverPresetAtivo(esteMes.dataInicio, esteMes.dataFim, 'Este mês')).toBe('Este mês');
  });
});

function range(label: string) {
  const preset = DATE_RANGE_PRESETS.find((item) => item.label === label);
  if (!preset) {
    throw new Error(`Preset ${label} nao encontrado`);
  }
  return preset.getRange();
}
