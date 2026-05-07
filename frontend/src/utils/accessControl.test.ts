import { describe, expect, it } from 'vitest';
import { createEmptyPermissionMap, firstAccessibleRoute } from './accessControl';

describe('accessControl', () => {
  it('inclui a permissão indicadoresGestaoAVista no mapa vazio', () => {
    expect(createEmptyPermissionMap()).toMatchObject({
      indicadoresGestaoAVista: false,
    });
  });

  it('redireciona usuários autenticados para a Home', () => {
    expect(firstAccessibleRoute({
      papel: 'usuario_comum',
      exigeTrocaSenha: false,
      permissoesEfetivas: {
        ...createEmptyPermissionMap(),
        indicadoresGestaoAVista: true,
      },
    })).toBe('/');
  });

  it('mantém troca obrigatória de senha antes da Home', () => {
    expect(firstAccessibleRoute({
      papel: 'usuario_comum',
      exigeTrocaSenha: true,
      permissoesEfetivas: createEmptyPermissionMap(),
    })).toBe('/alterar-senha');
  });

  it('redireciona sessão ausente para login', () => {
    expect(firstAccessibleRoute(null)).toBe('/login');
  });
});
