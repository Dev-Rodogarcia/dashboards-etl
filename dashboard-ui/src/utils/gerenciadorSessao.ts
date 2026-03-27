import type { IUsuarioSessao, LoginResponse, UsuarioSessao } from '../types/auth';

const CHAVE_SESSAO = 'dashboard_usuario';
export const EVENTO_SESSAO_ATUALIZADA = 'dashboard:sessao-atualizada';

export function montarSessaoPersistida(dados: UsuarioSessao, token: string, exigeTrocaSenhaFallback = false): IUsuarioSessao {
  return {
    ...dados,
    token,
    exigeTrocaSenha: dados.exigeTrocaSenha ?? exigeTrocaSenhaFallback,
  };
}

export function montarSessaoDoLogin(resposta: LoginResponse): IUsuarioSessao {
  return montarSessaoPersistida(
    resposta.usuario,
    resposta.token,
    resposta.exigeTrocaSenha,
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

function normalizarSessaoPersistida(sessao: Partial<IUsuarioSessao> | null): IUsuarioSessao | null {
  if (!sessao?.token || !sessao?.papel) return null;

  return {
    ...sessao,
    exigeTrocaSenha: Boolean(sessao.exigeTrocaSenha),
    filiaisPermitidasEfetivas: Array.isArray(sessao.filiaisPermitidasEfetivas) ? sessao.filiaisPermitidasEfetivas : [],
  } as IUsuarioSessao;
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
  const sessionStorage = obterSessionStorage();
  sessionStorage?.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
  obterLocalStorage()?.removeItem(CHAVE_SESSAO);
  notificarMudancaSessao();
}

export function obterSessao(): IUsuarioSessao | null {
  const sessionStorage = obterSessionStorage();
  const sessaoAtual = lerSessaoStorage(sessionStorage);
  if (sessaoAtual) {
    return sessaoAtual;
  }

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
  notificarMudancaSessao();
}
