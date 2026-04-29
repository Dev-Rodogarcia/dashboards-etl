/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  alterarSenha as alterarSenhaServico,
  buscarSessaoAtual,
  loginUsuario,
  logoutUsuario,
  restaurarSessao,
} from '../api/endpoints/authServico';
import type {
  AlterarSenhaRequest,
  IUsuarioSessao,
  LoginRequest,
  LoginResponse,
} from '../types/auth';
import {
  limparSessao,
  EVENTO_SESSAO_ATUALIZADA,
  montarSessaoDoLogin,
  montarSessaoPersistida,
  obterSessao,
  salvarSessao,
  sessaoExpirada,
} from '../utils/gerenciadorSessao';
import { resolverAcaoBootstrapSessao } from '../utils/authSession';

interface AutenticacaoContexto {
  usuario: IUsuarioSessao | null;
  carregandoSessao: boolean;
  login: (credenciais: LoginRequest) => Promise<LoginResponse>;
  alterarSenha: (payload: AlterarSenhaRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AutenticacaoContext = createContext<AutenticacaoContexto | null>(null);
const ANTECEDENCIA_REFRESH_MS = 60 * 1000;
const INTERVALO_RETRY_REFRESH_MS = 60 * 1000;

function extrairExpJwtMs(token: string): number | null {
  const [, payloadBase64] = token.split('.');
  if (!payloadBase64) return null;

  try {
    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64)) as { exp?: number };
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function calcularDelayRefresh(sessao: IUsuarioSessao): number {
  const sessaoExpiraEmMs = Date.parse(sessao.sessaoExpiraEm);
  const tokenExpiraEmMs = extrairExpJwtMs(sessao.token);
  const agora = Date.now();

  if (!Number.isFinite(sessaoExpiraEmMs)) {
    return 0;
  }

  if (tokenExpiraEmMs == null) {
    return Math.max(0, Math.min(INTERVALO_RETRY_REFRESH_MS, sessaoExpiraEmMs - agora));
  }

  return Math.max(0, Math.min(tokenExpiraEmMs - ANTECEDENCIA_REFRESH_MS, sessaoExpiraEmMs) - agora);
}

export function AutenticacaoProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<IUsuarioSessao | null>(() => obterSessao());
  const [carregandoSessao, setCarregandoSessao] = useState(true);

  useEffect(() => {
    function sincronizarSessaoAtual() {
      setUsuario(obterSessao());
    }

    window.addEventListener(EVENTO_SESSAO_ATUALIZADA, sincronizarSessaoAtual);
    return () => {
      window.removeEventListener(EVENTO_SESSAO_ATUALIZADA, sincronizarSessaoAtual);
    };
  }, []);

  useEffect(() => {
    let ativo = true;

    async function bootstrapSessao() {
      const sessao = obterSessao();
      if (!sessao?.token) {
        try {
          const restaurada = montarSessaoDoLogin(await restaurarSessao());
          if (!ativo) return;

          salvarSessao(restaurada);
          setUsuario(restaurada);
        } catch {
          if (!ativo) return;

          limparSessao();
          setUsuario(null);
        } finally {
          if (ativo) setCarregandoSessao(false);
        }
        return;
      }

      try {
        const dados = await buscarSessaoAtual();
        if (!ativo) return;

        // Re-lê o token do storage: o interceptor pode ter feito refresh silencioso
        // durante a chamada acima, atualizando o token. Usar sessao.token (capturado
        // antes do await) sobrescreveria o token novo com o expirado.
        const sessaoAtualizada = obterSessao();
        const tokenAtual = sessaoAtualizada?.token ?? sessao.token;
        const atualizada = montarSessaoPersistida(dados, tokenAtual, sessao.exigeTrocaSenha, sessao.sessaoExpiraEm);
        salvarSessao(atualizada);
        setUsuario(atualizada);
      } catch (error) {
        if (!ativo) return;

        if (resolverAcaoBootstrapSessao(error) === 'encerrar_sessao') {
          limparSessao();
          setUsuario(null);
        } else {
          setUsuario(sessao);
        }
      } finally {
        if (ativo) setCarregandoSessao(false);
      }
    }

    void bootstrapSessao();

    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    if (!usuario?.token) return undefined;

    let cancelado = false;
    const timeoutId = window.setTimeout(() => {
      async function executarRefreshProgramado() {
        const sessaoAtual = obterSessao();
        if (!sessaoAtual || cancelado) return;

        if (sessaoExpirada(sessaoAtual)) {
          limparSessao();
          setUsuario(null);
          setCarregandoSessao(false);
          return;
        }

        try {
          const renovada = montarSessaoDoLogin(await restaurarSessao());
          if (cancelado) return;
          salvarSessao(renovada);
          setUsuario(renovada);
        } catch (error) {
          if (cancelado) return;
          if (resolverAcaoBootstrapSessao(error) === 'encerrar_sessao') {
            limparSessao();
            setUsuario(null);
          }
        }
      }

      void executarRefreshProgramado();
    }, calcularDelayRefresh(usuario));

    return () => {
      cancelado = true;
      window.clearTimeout(timeoutId);
    };
  }, [usuario?.token, usuario?.sessaoExpiraEm]);

  const login = useCallback(async (credenciais: LoginRequest): Promise<LoginResponse> => {
    const data = await loginUsuario(credenciais);
    const sessao = montarSessaoDoLogin(data);
    salvarSessao(sessao);
    setUsuario(sessao);
    setCarregandoSessao(false);
    return data;
  }, []);

  const alterarSenha = useCallback(async (payload: AlterarSenhaRequest) => {
    await alterarSenhaServico(payload);

    setUsuario((atual) => {
      if (!atual) return atual;
      const proximaSessao = { ...atual, exigeTrocaSenha: false };
      salvarSessao(proximaSessao);
      return proximaSessao;
    });
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutUsuario();
    } finally {
      limparSessao();
      setUsuario(null);
      setCarregandoSessao(false);
    }
  }, []);

  return (
    <AutenticacaoContext.Provider value={{ usuario, carregandoSessao, login, alterarSenha, logout }}>
      {children}
    </AutenticacaoContext.Provider>
  );
}

export function useAutenticacao(): AutenticacaoContexto {
  const ctx = useContext(AutenticacaoContext);
  if (!ctx) throw new Error('useAutenticacao deve ser usado dentro de AutenticacaoProvider');
  return ctx;
}
