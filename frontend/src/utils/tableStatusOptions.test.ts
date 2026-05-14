import { describe, expect, it } from 'vitest';
import { combinarStatusOptions } from './tableStatusOptions';

describe('combinarStatusOptions', () => {
  it('combina status carregados, selecionados e presentes na tabela sem duplicar vazios', () => {
    expect(combinarStatusOptions(
      ['Entregue', 'Pendente'],
      ['pendente', '', null],
      ['Cancelado', undefined],
    )).toEqual(['Cancelado', 'Entregue', 'Pendente']);
  });
});
