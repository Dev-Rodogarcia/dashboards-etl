import type { IUsuarioSessao } from '../types/auth';

const CHAVE_SESSAO = 'dashboard_usuario';

export function salvarSessao(usuario: IUsuarioSessao): void {
  sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
}

export function obterSessao(): IUsuarioSessao | null {
  const dados = sessionStorage.getItem(CHAVE_SESSAO);
  if (!dados) return null;

  const sessao = JSON.parse(dados) as Partial<IUsuarioSessao>;
  if (!sessao?.token) return null;

  return {
    ...sessao,
    papeis: Array.isArray(sessao.papeis) ? sessao.papeis : [],
    exigeTrocaSenha: Boolean(sessao.exigeTrocaSenha),
  } as IUsuarioSessao;
}

export function limparSessao(): void {
  sessionStorage.removeItem(CHAVE_SESSAO);
}
