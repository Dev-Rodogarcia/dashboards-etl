import { describe, expect, it } from 'vitest';
import { aplicarFiltrosTabelaParams } from './tableFilters';

describe('aplicarFiltrosTabelaParams', () => {
  it('traduz filtros de tabela para parametros f.tabela* sem usar t_*', () => {
    const params = new URLSearchParams();

    aplicarFiltrosTabelaParams(params, {
      tabelaBusca: 'ACME',
      tabelaCodigo: '123',
      tabelaStatus: ['Faturado', 'Pendente'],
      tabelaColuna: {
        valorFrete: '150.50',
        status: ['Entregue', 'Pendente'],
      },
    });

    expect(params.get('f.tabelaBusca')).toBe('ACME');
    expect(params.get('f.tabelaCodigo')).toBe('123');
    expect(params.getAll('f.tabelaStatus')).toEqual(['Faturado', 'Pendente']);
    expect(params.get('f.tabelaColuna.valorFrete')).toBe('150.50');
    expect(params.getAll('f.tabelaColuna.status')).toEqual(['Entregue', 'Pendente']);
    expect(params.has('t_busca')).toBe(false);
    expect(params.has('t_col_valorFrete')).toBe(false);
  });
});
