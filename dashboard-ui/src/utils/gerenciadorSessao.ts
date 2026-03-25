import type { IUsuarioSessao, LoginResponse, UsuarioSessao } from '../types/auth';

const CHAVE_SESSAO = 'dashboard_usuario';

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

export function salvarSessao(usuario: IUsuarioSessao): void {
  localStorage.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
}

export function obterSessao(): IUsuarioSessao | null {
  const dados = localStorage.getItem(CHAVE_SESSAO);
  if (!dados) return null;

  const sessao = JSON.parse(dados) as Partial<IUsuarioSessao>;
  if (!sessao?.token || !sessao?.papel) return null;

  return {
    ...sessao,
    exigeTrocaSenha: Boolean(sessao.exigeTrocaSenha),
    filiaisPermitidasEfetivas: Array.isArray(sessao.filiaisPermitidasEfetivas) ? sessao.filiaisPermitidasEfetivas : [],
  } as IUsuarioSessao;
}

export function atualizarTokenSessao(token: string): IUsuarioSessao | null {
  const sessao = obterSessao();
  if (!sessao) return null;

  const proximaSessao = { ...sessao, token };
  salvarSessao(proximaSessao);
  return proximaSessao;
}

export function limparSessao(): void {
  localStorage.removeItem(CHAVE_SESSAO);
}
