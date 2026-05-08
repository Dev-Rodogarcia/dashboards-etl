import type { IUsuarioSessao, LoginResponse, UsuarioSessao } from '../types/auth';

const CHAVE_SESSAO = 'dashboard_usuario';
const CHAVE_REFRESH_ATIVO = 'dashboard_refresh_ativo';
export const EVENTO_SESSAO_ATUALIZADA = 'dashboard:sessao-atualizada';
const TOLERANCIA_EXPIRACAO_SESSAO_MS = 2 * 60 * 1000;

export function montarSessaoPersistida(
  dados: UsuarioSessao,
  token: string,
  exigeTrocaSenhaFallback = false,
  sessaoExpiraEm = '',
): IUsuarioSessao {
  return {
    ...dados,
    token,
    sessaoExpiraEm,
    exigeTrocaSenha: dados.exigeTrocaSenha ?? exigeTrocaSenhaFallback,
  };
}

export function montarSessaoDoLogin(resposta: LoginResponse): IUsuarioSessao {
  return montarSessaoPersistida(
    resposta.usuario,
    resposta.token,
    resposta.exigeTrocaSenha,
    resposta.sessaoExpiraEm,
  );
}

function obterSessionStorage(): Storage | null {
  return typeof globalThis.sessionStorage === 'undefined' ? null : globalThis.sessionStorage;
}

function obterLocalStorage(): Storage | null {
  return typeof globalThis.localStorage === 'undefined' ? null : globalThis.localStorage;
}

function notificarMudancaSessao(): void {
  if (typeof window === 'undefined' || typeof window.dispatchEvent !== 'function') {
    return;
  }

  window.dispatchEvent(new Event(EVENTO_SESSAO_ATUALIZADA));
}

function dataValida(valor: unknown): valor is string {
  return typeof valor === 'string' && Number.isFinite(Date.parse(valor));
}

export function sessaoExpirada(sessao: Pick<IUsuarioSessao, 'sessaoExpiraEm'>): boolean {
  const expiraEmMs = Date.parse(sessao.sessaoExpiraEm);
  return Number.isFinite(expiraEmMs) && expiraEmMs + TOLERANCIA_EXPIRACAO_SESSAO_MS <= Date.now();
}

function normalizarSessaoPersistida(sessao: Partial<IUsuarioSessao> | null): IUsuarioSessao | null {
  if (!sessao?.token || !sessao?.papel || !dataValida(sessao.sessaoExpiraEm)) return null;

  const sessaoNormalizada = {
    ...sessao,
    sessaoExpiraEm: sessao.sessaoExpiraEm,
    exigeTrocaSenha: Boolean(sessao.exigeTrocaSenha),
    filiaisPermitidasEfetivas: Array.isArray(sessao.filiaisPermitidasEfetivas) ? sessao.filiaisPermitidasEfetivas : [],
  } as IUsuarioSessao;

  return sessaoExpirada(sessaoNormalizada) ? null : sessaoNormalizada;
}

function lerSessaoStorage(storage: Storage | null): IUsuarioSessao | null {
  if (!storage) {
    return null;
  }

  const dados = storage.getItem(CHAVE_SESSAO);
  if (!dados) {
    return null;
  }

  try {
    const sessao = JSON.parse(dados) as Partial<IUsuarioSessao>;
    const sessaoNormalizada = normalizarSessaoPersistida(sessao);

    if (!sessaoNormalizada) {
      storage.removeItem(CHAVE_SESSAO);
    }

    return sessaoNormalizada;
  } catch {
    storage.removeItem(CHAVE_SESSAO);
    return null;
  }
}

export function salvarSessao(usuario: IUsuarioSessao): void {
  obterSessionStorage()?.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
  obterLocalStorage()?.setItem(CHAVE_REFRESH_ATIVO, '1');
  obterLocalStorage()?.removeItem(CHAVE_SESSAO);
  notificarMudancaSessao();
}

export function deveTentarRestaurarSessao(): boolean {
  return obterLocalStorage()?.getItem(CHAVE_REFRESH_ATIVO) === '1';
}

export function obterSessao(): IUsuarioSessao | null {
  const sessionStorage = obterSessionStorage();
  const sessaoAtual = lerSessaoStorage(sessionStorage);
  if (sessaoAtual) {
    return sessaoAtual;
  }

  // Migração: sessões antigas ainda no localStorage
  const localStorage = obterLocalStorage();
  const sessaoLegada = lerSessaoStorage(localStorage);
  if (!sessaoLegada) {
    return null;
  }

  if (sessionStorage) {
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessaoLegada));
  }
  localStorage?.removeItem(CHAVE_SESSAO);

  return sessaoLegada;
}

export function atualizarTokenSessao(token: string): IUsuarioSessao | null {
  const sessao = obterSessao();
  if (!sessao) return null;

  const proximaSessao = { ...sessao, token };
  salvarSessao(proximaSessao);
  return proximaSessao;
}

export function limparSessao(): void {
  obterSessionStorage()?.removeItem(CHAVE_SESSAO);
  obterLocalStorage()?.removeItem(CHAVE_SESSAO);
  obterLocalStorage()?.removeItem(CHAVE_REFRESH_ATIVO);
  notificarMudancaSessao();
}
