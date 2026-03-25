import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../config/api';
import type { LoginResponse } from '../types/auth';
import { limparSessao, montarSessaoDoLogin, obterSessao, salvarSessao } from '../utils/gerenciadorSessao';

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

const clienteAxios = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

let refreshEmAndamento: Promise<LoginResponse> | null = null;

clienteAxios.interceptors.request.use((config) => {
  const sessao = obterSessao();
  if (sessao?.token) {
    config.headers.Authorization = `Bearer ${sessao.token}`;
  }
  return config;
});

async function renovarSessaoSilenciosamente(): Promise<LoginResponse> {
  const { data } = await axios.post<LoginResponse>(
    `${API_BASE_URL}/api/auth/refresh`,
    {},
    {
      withCredentials: true,
      headers: { 'Content-Type': 'application/json' },
    },
  );

  salvarSessao(montarSessaoDoLogin(data));
  return data;
}

export async function renovarSessao(): Promise<LoginResponse> {
  if (!refreshEmAndamento) {
    refreshEmAndamento = renovarSessaoSilenciosamente().finally(() => {
      refreshEmAndamento = null;
    });
  }

  return refreshEmAndamento;
}

clienteAxios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const url = String(originalRequest?.url ?? '');
    const ehEndpointAuth = url.includes('/api/auth/login') || url.includes('/api/auth/refresh') || url.includes('/api/auth/logout');

    if (status === 401 && originalRequest && !originalRequest._retry && !ehEndpointAuth) {
      originalRequest._retry = true;

      try {
        const resposta = await renovarSessao();
        originalRequest.headers.Authorization = `Bearer ${resposta.token}`;
        return clienteAxios(originalRequest);
      } catch {
        limparSessao();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }

    if (status === 401 && ehEndpointAuth) {
      limparSessao();
    }

    if (status === 403) {
      if (window.location.pathname !== '/acesso-negado') {
        window.location.href = '/acesso-negado';
      }
    }

    return Promise.reject(error);
  },
);

export default clienteAxios;
