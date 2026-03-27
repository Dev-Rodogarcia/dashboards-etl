import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../config/api';
import type { LoginResponse } from '../types/auth';
import { limparSessao, montarSessaoDoLogin, obterSessao, salvarSessao } from '../utils/gerenciadorSessao';
import { ehSessaoExpiradaError, normalizarErroSessao } from '../utils/authSession';

export interface RetryableRequestConfig extends InternalAxiosRequestConfig {
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
  try {
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
  } catch (error) {
    throw normalizarErroSessao(error);
  }
}

export async function renovarSessao(): Promise<LoginResponse> {
  if (!refreshEmAndamento) {
    refreshEmAndamento = renovarSessaoSilenciosamente().finally(() => {
      refreshEmAndamento = null;
    });
  }

  return refreshEmAndamento;
}

interface TratamentoErroRespostaDeps {
  renovarSessao: () => Promise<LoginResponse>;
  repetirRequisicao: (config: RetryableRequestConfig) => Promise<unknown>;
  limparSessao: () => void;
  obterPathAtual: () => string;
  redirecionar: (path: string) => void;
}

function obterUrlRequisicao(config?: RetryableRequestConfig): string {
  return String(config?.url ?? '');
}

function ehEndpointAuth(url: string): boolean {
  return url.includes('/api/auth/login') || url.includes('/api/auth/refresh') || url.includes('/api/auth/logout');
}

function encerrarSessaoLocal(deps: Pick<TratamentoErroRespostaDeps, 'limparSessao' | 'obterPathAtual' | 'redirecionar'>): void {
  deps.limparSessao();
  if (deps.obterPathAtual() !== '/login') {
    deps.redirecionar('/login');
  }
}

export async function tratarErroRespostaApi(
  error: unknown,
  deps: TratamentoErroRespostaDeps,
): Promise<unknown> {
  const resposta = error as {
    response?: { status?: number };
    config?: RetryableRequestConfig;
  };
  const status = resposta.response?.status;
  const originalRequest = resposta.config;
  const url = obterUrlRequisicao(originalRequest);

  if (status === 401 && originalRequest && !originalRequest._retry && !ehEndpointAuth(url)) {
    originalRequest._retry = true;

    try {
      const sessaoRenovada = await deps.renovarSessao();
      originalRequest.headers = originalRequest.headers ?? {};
      originalRequest.headers.Authorization = `Bearer ${sessaoRenovada.token}`;
      return deps.repetirRequisicao(originalRequest);
    } catch (refreshError) {
      if (ehSessaoExpiradaError(refreshError)) {
        encerrarSessaoLocal(deps);
      }

      return Promise.reject(refreshError);
    }
  }

  if (status === 403) {
    if (deps.obterPathAtual() !== '/acesso-negado') {
      deps.redirecionar('/acesso-negado');
    }
  }

  return Promise.reject(error);
}

clienteAxios.interceptors.response.use(
  (response) => response,
  (error) => tratarErroRespostaApi(error, {
    renovarSessao,
    repetirRequisicao: (config) => clienteAxios(config),
    limparSessao,
    obterPathAtual: () => window.location.pathname,
    redirecionar: (path) => {
      window.location.href = path;
    },
  }),
);

export default clienteAxios;
