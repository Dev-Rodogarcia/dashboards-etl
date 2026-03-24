import clienteAxios from '../clienteAxios';
import type {
  AlterarSenhaRequest,
  LoginRequest,
  LoginResponse,
  UsuarioSessao,
} from '../../types/auth';

export async function loginUsuario(credenciais: LoginRequest): Promise<LoginResponse> {
  const { data } = await clienteAxios.post<LoginResponse>('/api/auth/login', credenciais);
  return data;
}

export async function buscarSessaoAtual(): Promise<UsuarioSessao> {
  const { data } = await clienteAxios.get<UsuarioSessao>('/api/auth/me');
  return data;
}

export async function alterarSenha(payload: AlterarSenhaRequest): Promise<void> {
  await clienteAxios.post('/api/auth/alterar-senha', payload);
}
