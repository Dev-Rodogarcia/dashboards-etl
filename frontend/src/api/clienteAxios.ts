import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL, API_REQUEST_TIMEOUT_MS, AUTH_REQUEST_TIMEOUT_MS } from '../config/api';
import type { LoginResponse } from '../types/auth';
import { limparSessao, obterAccessToken, salvarSessaoDoLogin } from '../utils/gerenciadorSessao';
import { SessaoExpiradaError, normalizarErroSessao } from '../utils/authSession';
import { DATABASE_TIMEOUT_MESSAGE, SERVER_INSTABILITY_MESSAGE } from '../utils/apiError';

export interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

export const API_STATUS_ALERT_EVENT = 'dashboard:api-status-alert';

export interface ApiStatusAlertDetail {
  status?: number;
  mensagem: string;
  tipo: 'timeout' | 'indisponivel';
}

const clienteAxios = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: API_REQUEST_TIMEOUT_MS,
  withCredentials: true,
});

let refreshEmAndamento: Promise<LoginResponse> | null = null;
let ultimoAlertaInfraestrutura: { chave: string; timestamp: number } | null = null;
const API_STATUS_ALERT_COOLDOWN_MS = 5000;

clienteAxios.interceptors.request.use((config) => {
  const accessToken = obterAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
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
        timeout: AUTH_REQUEST_TIMEOUT_MS,
        headers: { 'Content-Type': 'application/json' },
      },
    );

    salvarSessaoDoLogin(data);
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
  limparSessao: () => void;
  revogarSessaoRemota?: () => Promise<void>;
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

async function revogarSessaoRemota(): Promise<void> {
  const logoutUrl = `${API_BASE_URL}/api/auth/logout`;

  try {
    if (typeof fetch === 'function') {
      await fetch(logoutUrl, {
        method: 'POST',
        credentials: 'include',
        keepalive: true,
      });
      return;
    }

    await axios.post(
      logoutUrl,
      {},
      {
        withCredentials: true,
        timeout: AUTH_REQUEST_TIMEOUT_MS,
        headers: { 'Content-Type': 'application/json' },
      },
    );
  } catch {
    // A sessao local ja foi encerrada; falha remota nao deve impedir o redirecionamento.
  }
}

function obterAlertaInfraestrutura(status?: number): ApiStatusAlertDetail | null {
  if (status === 503) {
    return { status, mensagem: SERVER_INSTABILITY_MESSAGE, tipo: 'indisponivel' };
  }

  if (status === 504) {
    return { status, mensagem: DATABASE_TIMEOUT_MESSAGE, tipo: 'timeout' };
  }

  return null;
}

function notificarErroInfraestrutura(status?: number): void {
  if (typeof window === 'undefined') {
    return;
  }

  const alerta = obterAlertaInfraestrutura(status);
  if (!alerta) {
    return;
  }

  const agora = Date.now();
  const chave = `${alerta.status}:${alerta.mensagem}`;
  if (
    ultimoAlertaInfraestrutura?.chave === chave
    && agora - ultimoAlertaInfraestrutura.timestamp < API_STATUS_ALERT_COOLDOWN_MS
  ) {
    return;
  }

  ultimoAlertaInfraestrutura = { chave, timestamp: agora };
  window.dispatchEvent(new CustomEvent<ApiStatusAlertDetail>(API_STATUS_ALERT_EVENT, { detail: alerta }));
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

  notificarErroInfraestrutura(status);

  if (status === 401 && !ehEndpointAuth(url)) {
    void deps.revogarSessaoRemota?.();
    encerrarSessaoLocal(deps);
    return Promise.reject(new SessaoExpiradaError(error));
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
    limparSessao,
    revogarSessaoRemota,
    obterPathAtual: () => window.location.pathname,
    redirecionar: (path) => {
      window.location.href = path;
    },
  }),
);

export default clienteAxios;
