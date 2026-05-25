import { beforeEach, describe, expect, it } from 'vitest';
import {
  deveTentarRestaurarSessao,
  EVENTO_SESSAO_ATUALIZADA,
  limparSessao,
  obterAccessToken,
  obterSessao,
  salvarSessao,
} from './gerenciadorSessao';
import type { IUsuarioSessao } from '../types/auth';
import type { PermissionMap } from '../types/access';
import { createEmptyPermissionMap } from './accessControl';

class StorageMock implements Storage {
  private readonly store = new Map<string, string>();

  get length(): number {
    return this.store.size;
  }

  clear(): void {
    this.store.clear();
  }

  getItem(key: string): string | null {
    return this.store.has(key) ? this.store.get(key) ?? null : null;
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.store.delete(key);
  }

  setItem(key: string, value: string): void {
    this.store.set(key, value);
  }
}

function criarWindowMock() {
  const listeners = new Map<string, Set<(event: Event) => void>>();
  const listenerMap = new WeakMap<EventListenerOrEventListenerObject, (event: Event) => void>();

  return {
    location: { pathname: '/coletas', href: '/coletas' },
    addEventListener: (nome: string, listener: EventListenerOrEventListenerObject) => {
      const callback = typeof listener === 'function'
        ? listener
        : (event: Event) => listener.handleEvent(event);
      listenerMap.set(listener, callback);
      listeners.set(nome, (listeners.get(nome) ?? new Set()).add(callback));
    },
    removeEventListener: (nome: string, listener: EventListenerOrEventListenerObject) => {
      const callbacks = listeners.get(nome);
      const callback = listenerMap.get(listener);
      if (callbacks && callback) {
        callbacks.delete(callback);
      }
    },
    dispatchEvent: (event: Event) => {
      for (const callback of listeners.get(event.type) ?? []) {
        callback(event);
      }
      return true;
    },
  };
}

function criarPermissoes(): PermissionMap {
  return {
    ...createEmptyPermissionMap(),
    coletas: true,
  };
}

function criarSessao(): IUsuarioSessao {
  return {
    id: '1',
    nome: 'Painel',
    email: 'painel@empresa.com',
    papel: 'usuario_comum',
    setor: { id: '10', nome: 'Operacoes' },
    permissoesEfetivas: criarPermissoes(),
    filiaisPermitidasEfetivas: ['SP'],
    exigeTrocaSenha: false,
    sessaoExpiraEm: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  };
}

beforeEach(() => {
  Object.defineProperty(globalThis, 'sessionStorage', {
    value: new StorageMock(),
    configurable: true,
  });
  Object.defineProperty(globalThis, 'localStorage', {
    value: new StorageMock(),
    configurable: true,
  });
  Object.defineProperty(globalThis, 'window', {
    value: criarWindowMock(),
    configurable: true,
  });
  limparSessao();
});

describe('gerenciadorSessao', () => {
  it('salva a sessao em memoria, guarda o token fora do Web Storage e remove legado', () => {
    const sessao = criarSessao();
    localStorage.setItem('dashboard_usuario', JSON.stringify({ legado: true }));

    salvarSessao(sessao, 'token-inicial');

    expect(obterSessao()).toEqual(sessao);
    expect(obterAccessToken()).toBe('token-inicial');
    expect(sessionStorage.getItem('dashboard_usuario')).toBeNull();
    expect(localStorage.getItem('dashboard_refresh_ativo')).toBe('1');
    expect(localStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('remove sessao legada do localStorage sem reidratar token para forcar refresh HttpOnly', () => {
    const sessao = criarSessao();
    localStorage.setItem('dashboard_usuario', JSON.stringify({ ...sessao, token: 'token-legado' }));
    localStorage.setItem('dashboard_refresh_ativo', '1');

    const resultado = obterSessao();

    expect(resultado).toBeNull();
    expect(obterAccessToken()).toBeNull();
    expect(deveTentarRestaurarSessao()).toBe(true);
    expect(sessionStorage.getItem('dashboard_usuario')).toBeNull();
    expect(localStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('limpa memoria, sessionStorage e localStorage ao encerrar a sessao', () => {
    const sessao = criarSessao();
    localStorage.setItem('dashboard_usuario', JSON.stringify({ ...sessao, token: 'token-legado' }));
    sessionStorage.setItem('dashboard_usuario', JSON.stringify({ ...sessao, token: 'token-legado' }));
    salvarSessao(sessao, 'token-inicial');

    limparSessao();

    expect(obterSessao()).toBeNull();
    expect(obterAccessToken()).toBeNull();
    expect(localStorage.getItem('dashboard_usuario')).toBeNull();
    expect(localStorage.getItem('dashboard_refresh_ativo')).toBeNull();
    expect(sessionStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('só tenta restaurar por refresh quando existe marcador local de sessao', () => {
    expect(deveTentarRestaurarSessao()).toBe(false);

    salvarSessao(criarSessao(), 'token-inicial');

    expect(deveTentarRestaurarSessao()).toBe(true);

    limparSessao();

    expect(deveTentarRestaurarSessao()).toBe(false);
  });

  it('descarta sessao legada sem expiracao absoluta para forcar restauracao por refresh', () => {
    localStorage.setItem('dashboard_usuario', JSON.stringify({
      ...criarSessao(),
      token: 'token-legado',
      sessaoExpiraEm: undefined,
    }));

    expect(obterSessao()).toBeNull();
    expect(localStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('dispara evento interno quando a sessao e atualizada ou removida', () => {
    const eventos: string[] = [];
    window.addEventListener(EVENTO_SESSAO_ATUALIZADA, () => {
      eventos.push(EVENTO_SESSAO_ATUALIZADA);
    });

    salvarSessao(criarSessao(), 'token-inicial');
    limparSessao();

    expect(eventos).toEqual([EVENTO_SESSAO_ATUALIZADA, EVENTO_SESSAO_ATUALIZADA]);
  });
});
