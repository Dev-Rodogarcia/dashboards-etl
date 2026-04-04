import { beforeEach, describe, expect, it } from 'vitest';
import {
  EVENTO_SESSAO_ATUALIZADA,
  limparSessao,
  obterSessao,
  salvarSessao,
} from './gerenciadorSessao';
import type { IUsuarioSessao } from '../types/auth';
import type { PermissionMap } from '../types/access';

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
    coletas: true,
    manifestos: false,
    fretes: false,
    tracking: false,
    faturas: false,
    faturasPorCliente: false,
    contasAPagar: false,
    cotacoes: false,
    indicadoresGestaoAVista: false,
    executivo: false,
    etlSaude: false,
    dimensoes: false,
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
    token: 'token-inicial',
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
});

describe('gerenciadorSessao', () => {
  it('salva a sessao no localStorage e remove legado do sessionStorage', () => {
    const sessao = criarSessao();
    sessionStorage.setItem('dashboard_usuario', JSON.stringify({ legado: true }));

    salvarSessao(sessao);

    expect(localStorage.getItem('dashboard_usuario')).toBe(JSON.stringify(sessao));
    expect(sessionStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('migra a sessao legada do sessionStorage para o localStorage na primeira leitura', () => {
    const sessao = criarSessao();
    sessionStorage.setItem('dashboard_usuario', JSON.stringify(sessao));

    const resultado = obterSessao();

    expect(resultado).toEqual(sessao);
    expect(localStorage.getItem('dashboard_usuario')).toBe(JSON.stringify(sessao));
    expect(sessionStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('limpa sessionStorage e localStorage ao encerrar a sessao', () => {
    const sessao = criarSessao();
    localStorage.setItem('dashboard_usuario', JSON.stringify(sessao));
    sessionStorage.setItem('dashboard_usuario', JSON.stringify(sessao));

    limparSessao();

    expect(localStorage.getItem('dashboard_usuario')).toBeNull();
    expect(sessionStorage.getItem('dashboard_usuario')).toBeNull();
  });

  it('dispara evento interno quando a sessao e atualizada ou removida', () => {
    const eventos: string[] = [];
    window.addEventListener(EVENTO_SESSAO_ATUALIZADA, () => {
      eventos.push(EVENTO_SESSAO_ATUALIZADA);
    });

    salvarSessao(criarSessao());
    limparSessao();

    expect(eventos).toEqual([EVENTO_SESSAO_ATUALIZADA, EVENTO_SESSAO_ATUALIZADA]);
  });
});
