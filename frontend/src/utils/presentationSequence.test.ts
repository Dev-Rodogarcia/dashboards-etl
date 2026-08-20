import { describe, expect, it } from 'vitest';
import { filterPresentationPages } from './presentationSequence';

describe('filterPresentationPages', () => {
  it('mantém a ordem padrão para um usuário com todas as permissões', () => {
    expect(filterPresentationPages(() => true).map((page) => page.id)).toEqual([
      'faturamento', 'cotacoes', 'coletas', 'performance', 'manifestos', 'indicadores-gestao-a-vista',
    ]);
  });

  it('remove Cotações sem criar lacuna quando a permissão é negada', () => {
    expect(filterPresentationPages((permission) => permission !== 'cotacoes').map((page) => page.id)).toEqual([
      'faturamento', 'coletas', 'performance', 'manifestos', 'indicadores-gestao-a-vista',
    ]);
  });
});
