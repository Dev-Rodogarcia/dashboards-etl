import { describe, expect, it } from 'vitest';
import { formatarDataHora, formatarDataHoraMinuto } from './formatadores';

describe('formatadores de data/hora', () => {
  it('interpreta timestamp ISO sem fuso da API como UTC e renderiza em Sao Paulo', () => {
    expect(formatarDataHoraMinuto('2026-06-08T15:56:41')).toBe('08/06/2026 12:56');
  });

  it('preserva o mesmo instante quando timestamp ja vem com Z', () => {
    expect(formatarDataHoraMinuto('2026-06-08T15:56:41Z')).toBe('08/06/2026 12:56');
  });

  it('respeita offset explicito no timestamp', () => {
    expect(formatarDataHoraMinuto('2026-06-08T15:56:41-03:00')).toBe('08/06/2026 15:56');
  });

  it('mantem segundos no formatter completo', () => {
    expect(formatarDataHora('2026-06-08T15:56:41')).toBe('08/06/2026, 12:56:41');
  });

  it('devolve o valor original quando a data e invalida', () => {
    expect(formatarDataHoraMinuto('data-invalida')).toBe('data-invalida');
  });
});
