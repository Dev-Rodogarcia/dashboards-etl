import type { PermissionMap } from './access';

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface AlterarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
}

export interface NovaSenhaObrigatoriaRequest {
  email: string;
  senhaTemporaria: string;
  novaSenha: string;
}

export interface SolicitarRedefinicaoSenhaRequest {
  email: string;
}

export interface PasswordResetRequiredResponse {
  status: 'PASSWORD_RESET_REQUIRED';
  userId: string;
}

export interface SetorSessao {
  id: string;
  nome: string;
}

export interface UsuarioSessao {
  id: string;
  nome: string;
  email: string;
  papel: string;
  setor: SetorSessao;
  permissoesEfetivas: PermissionMap;
  filiaisPermitidasEfetivas: string[];
  exigeTrocaSenha: boolean;
}

export interface LoginResponse {
  usuario: UsuarioSessao;
  token: string;
  exigeTrocaSenha: boolean;
  sessaoExpiraEm: string;
}

export interface IUsuarioSessao extends UsuarioSessao {
  sessaoExpiraEm: string;
}
