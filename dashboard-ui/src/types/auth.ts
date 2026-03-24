import type { PermissionMap } from './access';

export interface LoginRequest {
  usuario: string;
  senha: string;
}

export interface AlterarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
}

export interface SetorSessao {
  id: string;
  nome: string;
  permissoes: PermissionMap;
}

export interface UsuarioSessao {
  id: string;
  login: string;
  nome: string;
  email: string;
  admin: boolean;
  setor: SetorSessao;
  papeis: string[];
  exigeTrocaSenha: boolean;
}

export interface LoginResponse {
  usuario: UsuarioSessao;
  token: string;
  exigeTrocaSenha: boolean;
}

export interface IUsuarioSessao extends UsuarioSessao {
  token: string;
}
