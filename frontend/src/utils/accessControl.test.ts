import { describe, expect, it } from 'vitest';
import { canAccess, createEmptyPermissionMap, firstAccessibleRoute } from './accessControl';

describe('accessControl', () => {
  it('inclui a permissão indicadoresGestaoAVista no mapa vazio', () => {
    expect(createEmptyPermissionMap()).toMatchObject({
      indicadoresGestaoAVista: false,
      homeComunicados: false,
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

  it('trata desenvolvedor@rodogarcia.com.br como acesso total', () => {
    expect(canAccess({
      id: 'dev',
      nome: 'Desenvolvedor',
      email: 'desenvolvedor@rodogarcia.com.br',
      papel: 'usuario_comum',
      setor: { id: '1', nome: 'TI' },
      permissoesEfetivas: createEmptyPermissionMap(),
      filiaisPermitidasEfetivas: [],
      exigeTrocaSenha: false,
      token: 'token',
      sessaoExpiraEm: new Date(Date.now() + 60_000).toISOString(),
    }, 'homeComunicados')).toBe(true);
  });
});
