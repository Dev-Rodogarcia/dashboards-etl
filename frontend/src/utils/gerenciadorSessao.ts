import type { IUsuarioSessao, LoginResponse, UsuarioSessao } from '../types/auth';

const CHAVE_SESSAO = 'dashboard_usuario';
const CHAVE_REFRESH_ATIVO = 'dashboard_refresh_ativo';
export const EVENTO_SESSAO_ATUALIZADA = 'dashboard:sessao-atualizada';
const TOLERANCIA_EXPIRACAO_SESSAO_MS = 2 * 60 * 1000;

const estadoSessao = (() => {
  let sessaoAtual: IUsuarioSessao | null = null;
  let accessTokenAtual: string | null = null;

  return {
    obterSessao: () => sessaoAtual,
    salvarSessao: (sessao: IUsuarioSessao) => {
      sessaoAtual = sessao;
    },
    obterAccessToken: () => accessTokenAtual,
    salvarAccessToken: (token: string) => {
      accessTokenAtual = token;
    },
    limpar: () => {
      sessaoAtual = null;
      accessTokenAtual = null;
    },
  };
})();

export function montarSessaoPersistida(
  dados: UsuarioSessao,
  exigeTrocaSenhaFallback = false,
  sessaoExpiraEm = '',
): IUsuarioSessao {
  return {
    ...dados,
    sessaoExpiraEm,
    exigeTrocaSenha: dados.exigeTrocaSenha ?? exigeTrocaSenhaFallback,
  };
}

export function montarSessaoDoLogin(resposta: LoginResponse): IUsuarioSessao {
  return montarSessaoPersistida(
    resposta.usuario,
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

export function sessaoExpirada(sessao: Pick<IUsuarioSessao, 'sessaoExpiraEm'>): boolean {
  const expiraEmMs = Date.parse(sessao.sessaoExpiraEm);
  return Number.isFinite(expiraEmMs) && expiraEmMs + TOLERANCIA_EXPIRACAO_SESSAO_MS <= Date.now();
}

function limparSessoesLegadas(): void {
  obterSessionStorage()?.removeItem(CHAVE_SESSAO);
  obterLocalStorage()?.removeItem(CHAVE_SESSAO);
}

export function obterAccessToken(): string | null {
  return estadoSessao.obterAccessToken();
}

export function salvarSessao(usuario: IUsuarioSessao, accessToken?: string): void {
  estadoSessao.salvarSessao(usuario);
  if (accessToken !== undefined) {
    estadoSessao.salvarAccessToken(accessToken);
  }

  limparSessoesLegadas();
  obterLocalStorage()?.setItem(CHAVE_REFRESH_ATIVO, '1');
  notificarMudancaSessao();
}

export function salvarSessaoDoLogin(resposta: LoginResponse): IUsuarioSessao {
  const sessao = montarSessaoDoLogin(resposta);
  salvarSessao(sessao, resposta.token);
  return sessao;
}

export function deveTentarRestaurarSessao(): boolean {
  return obterLocalStorage()?.getItem(CHAVE_REFRESH_ATIVO) === '1';
}

export function obterSessao(): IUsuarioSessao | null {
  limparSessoesLegadas();
  return estadoSessao.obterSessao();
}

export function atualizarTokenSessao(token: string): IUsuarioSessao | null {
  const sessao = obterSessao();
  if (!sessao) return null;

  estadoSessao.salvarAccessToken(token);
  notificarMudancaSessao();
  return sessao;
}

export function limparSessao(): void {
  estadoSessao.limpar();
  limparSessoesLegadas();
  obterLocalStorage()?.removeItem(CHAVE_REFRESH_ATIVO);
  notificarMudancaSessao();
}
