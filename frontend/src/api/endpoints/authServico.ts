import clienteAxios, { renovarSessao } from '../clienteAxios';
import { AUTH_REQUEST_TIMEOUT_MS } from '../../config/api';
import type {
  AlterarSenhaRequest,
  LoginRequest,
  LoginResponse,
  UsuarioSessao,
} from '../../types/auth';

const AUTH_REQUEST_CONFIG = {
  timeout: AUTH_REQUEST_TIMEOUT_MS,
};

export async function loginUsuario(credenciais: LoginRequest): Promise<LoginResponse> {
  const { data } = await clienteAxios.post<LoginResponse>('/api/auth/login', credenciais, AUTH_REQUEST_CONFIG);
  return data;
}

export async function buscarSessaoAtual(): Promise<UsuarioSessao> {
  const { data } = await clienteAxios.get<UsuarioSessao>('/api/auth/me', AUTH_REQUEST_CONFIG);
  return data;
}

export async function restaurarSessao(): Promise<LoginResponse> {
  return renovarSessao();
}

export async function logoutUsuario(): Promise<void> {
  await clienteAxios.post('/api/auth/logout', {}, AUTH_REQUEST_CONFIG);
}

export async function alterarSenha(payload: AlterarSenhaRequest): Promise<void> {
  await clienteAxios.post('/api/auth/alterar-senha', payload, AUTH_REQUEST_CONFIG);
}
