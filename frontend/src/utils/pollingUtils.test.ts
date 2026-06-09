import { describe, expect, it, vi } from 'vitest';
import {
  calcularIntervaloComJitter,
  OPERATIONAL_QUERY_POLLING_OPTIONS,
  POLLING_BASE_INTERVAL_MS,
  POLLING_MAX_JITTER_MS,
} from './pollingUtils';

describe('pollingUtils', () => {
  it('retorna o intervalo base quando o jitter aleatorio e zero', () => {
    expect(calcularIntervaloComJitter(() => 0)).toBe(1_800_000);
  });

  it('limita o jitter ao intervalo inteiro entre zero e 59.999 ms', () => {
    expect(calcularIntervaloComJitter(() => 0.999999)).toBe(
      POLLING_BASE_INTERVAL_MS + POLLING_MAX_JITTER_MS - 1,
    );
  });

  it('recalcula o jitter quando o React Query solicita o proximo intervalo', () => {
    const randomSpy = vi.spyOn(Math, 'random')
      .mockReturnValueOnce(0)
      .mockReturnValueOnce(0.5);

    expect(OPERATIONAL_QUERY_POLLING_OPTIONS.refetchInterval()).toBe(1_800_000);
    expect(OPERATIONAL_QUERY_POLLING_OPTIONS.refetchInterval()).toBe(1_830_000);

    randomSpy.mockRestore();
  });
});
