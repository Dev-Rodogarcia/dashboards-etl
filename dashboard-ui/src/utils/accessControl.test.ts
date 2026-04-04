import { describe, expect, it } from 'vitest';
import { createEmptyPermissionMap, firstAccessibleRoute } from './accessControl';

describe('accessControl', () => {
  it('inclui a permissão indicadoresGestaoAVista no mapa vazio', () => {
    expect(createEmptyPermissionMap()).toMatchObject({
      indicadoresGestaoAVista: false,
    });
  });

  it('redireciona para o novo dashboard quando for a primeira permissão disponível', () => {
    expect(firstAccessibleRoute({
      papel: 'usuario_comum',
      exigeTrocaSenha: false,
      permissoesEfetivas: {
        ...createEmptyPermissionMap(),
        indicadoresGestaoAVista: true,
      },
    })).toBe('/indicadores-gestao-a-vista');
  });
});
