import { describe, expect, it, vi, afterEach } from 'vitest';
import {
  adicionarDias,
  data30DiasAtrasLocal,
  dataHojeLocal,
  dataNDiasAtrasLocal,
  normalizarPeriodo,
} from './dateUtils';

// Congela o relógio em 2026-03-26 às 22:00 no fuso local (UTC-3 = 01:00 UTC do dia 27)
// Serve para validar que as funções usam hora local, não UTC.
const DATA_FROZEN = new Date('2026-03-27T01:00:00.000Z'); // 22h local UTC-3

describe('dataHojeLocal', () => {
  afterEach(() => vi.useRealTimers());

  it('retorna YYYY-MM-DD no fuso local — não UTC', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_FROZEN);
    // UTC seria 2026-03-27 mas local (UTC-3) é 2026-03-26
    expect(dataHojeLocal()).toBe('2026-03-26');
  });

  it('tem formato YYYY-MM-DD', () => {
    expect(dataHojeLocal()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});

describe('dataNDiasAtrasLocal', () => {
  afterEach(() => vi.useRealTimers());

  it('retorna 7 dias atrás no fuso local', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_FROZEN); // local = 2026-03-26
    expect(dataNDiasAtrasLocal(7)).toBe('2026-03-19');
  });

  it('retorna 30 dias atrás no fuso local', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_FROZEN);
    expect(dataNDiasAtrasLocal(30)).toBe('2026-02-24');
  });

  it('0 dias atrás = hoje', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_FROZEN);
    expect(dataNDiasAtrasLocal(0)).toBe(dataHojeLocal());
  });
});

describe('data30DiasAtrasLocal', () => {
  afterEach(() => vi.useRealTimers());

  it('equivale a dataNDiasAtrasLocal(30)', () => {
    vi.useFakeTimers();
    vi.setSystemTime(DATA_FROZEN);
    expect(data30DiasAtrasLocal()).toBe(dataNDiasAtrasLocal(30));
  });
});

describe('adicionarDias', () => {
  it('adiciona dias corretamente dentro do mesmo mês', () => {
    expect(adicionarDias('2026-03-10', 5)).toBe('2026-03-15');
  });

  it('atravessa virada de mês', () => {
    expect(adicionarDias('2026-03-28', 5)).toBe('2026-04-02');
  });

  it('atravessa virada de ano', () => {
    expect(adicionarDias('2025-12-28', 5)).toBe('2026-01-02');
  });

  it('ano bissexto — 29 de fevereiro', () => {
    expect(adicionarDias('2024-02-27', 2)).toBe('2024-02-29');
  });

  it('adicionar 0 dias retorna a mesma data', () => {
    expect(adicionarDias('2026-03-26', 0)).toBe('2026-03-26');
  });
});

describe('normalizarPeriodo', () => {
  it('retorna sem alteração quando o período é válido e dentro do limite', () => {
    const result = normalizarPeriodo('2026-03-01', '2026-03-26');
    expect(result).toEqual({ dataInicio: '2026-03-01', dataFim: '2026-03-26' });
  });

  it('troca início e fim quando início > fim', () => {
    const result = normalizarPeriodo('2026-03-26', '2026-03-01');
    expect(result).toEqual({ dataInicio: '2026-03-01', dataFim: '2026-03-26' });
  });

  it('trunca dataFim quando o período excede maxDias (padrão 366)', () => {
    const result = normalizarPeriodo('2026-01-01', '2028-01-01'); // 2 anos
    expect(result.dataInicio).toBe('2026-01-01');
    expect(result.dataFim).toBe(adicionarDias('2026-01-01', 366));
  });

  it('respeita maxDias personalizado', () => {
    const result = normalizarPeriodo('2026-01-01', '2026-06-01', 90); // passa 90 dias
    expect(result.dataFim).toBe(adicionarDias('2026-01-01', 90));
  });

  it('retorna sem alteração quando período é exatamente igual ao limite', () => {
    const fim = adicionarDias('2026-01-01', 90);
    const result = normalizarPeriodo('2026-01-01', fim, 90);
    expect(result).toEqual({ dataInicio: '2026-01-01', dataFim: fim });
  });

  it('retorna sem alteração quando datas estão vazias', () => {
    expect(normalizarPeriodo('', '2026-03-26')).toEqual({ dataInicio: '', dataFim: '2026-03-26' });
    expect(normalizarPeriodo('2026-03-01', '')).toEqual({ dataInicio: '2026-03-01', dataFim: '' });
  });

  it('aceita período de 1 dia (mesmo dia)', () => {
    const result = normalizarPeriodo('2026-03-26', '2026-03-26');
    expect(result).toEqual({ dataInicio: '2026-03-26', dataFim: '2026-03-26' });
  });
});
