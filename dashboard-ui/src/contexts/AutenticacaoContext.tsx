/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  alterarSenha as alterarSenhaServico,
  buscarSessaoAtual,
  loginUsuario,
  logoutUsuario,
} from '../api/endpoints/authServico';
import type {
  AlterarSenhaRequest,
  IUsuarioSessao,
  LoginRequest,
  LoginResponse,
} from '../types/auth';
import {
  limparSessao,
  montarSessaoDoLogin,
  montarSessaoPersistida,
  obterSessao,
  salvarSessao,
} from '../utils/gerenciadorSessao';

interface AutenticacaoContexto {
  usuario: IUsuarioSessao | null;
  carregandoSessao: boolean;
  login: (credenciais: LoginRequest) => Promise<LoginResponse>;
  alterarSenha: (payload: AlterarSenhaRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AutenticacaoContext = createContext<AutenticacaoContexto | null>(null);

export function AutenticacaoProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<IUsuarioSessao | null>(() => obterSessao());
  const [carregandoSessao, setCarregandoSessao] = useState(true);

  useEffect(() => {
    let ativo = true;

    async function bootstrapSessao() {
      const sessao = obterSessao();
      if (!sessao?.token) {
        setUsuario(null);
        setCarregandoSessao(false);
        return;
      }

      try {
        const dados = await buscarSessaoAtual();
        if (!ativo) return;

        const atualizada = montarSessaoPersistida(dados, sessao.token, sessao.exigeTrocaSenha);
        salvarSessao(atualizada);
        setUsuario(atualizada);
      } catch {
        if (!ativo) return;
        limparSessao();
        setUsuario(null);
      } finally {
        if (ativo) setCarregandoSessao(false);
      }
    }

    void bootstrapSessao();

    return () => {
      ativo = false;
    };
  }, []);

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
