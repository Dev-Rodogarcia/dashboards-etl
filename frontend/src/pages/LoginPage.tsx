import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { AxiosError } from 'axios';
import { useNavigate } from 'react-router-dom';
import { useTheme } from 'next-themes';
import { BarChart3, Eye, EyeOff, Loader2, Moon, ShieldCheck, Sun } from 'lucide-react';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import { firstAccessibleRoute } from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';
import { getPasswordPolicyErrors, PASSWORD_POLICY_HINT } from '../utils/passwordPolicy';

interface TrocaSenhaObrigatoriaPendente {
  email: string;
  senhaTemporaria: string;
}

function isPasswordResetRequiredError(error: unknown): boolean {
  if (!(error instanceof AxiosError) || error.response?.status !== 428) {
    return false;
  }

  const data = error.response.data as { status?: unknown } | undefined;
  return data?.status === 'PASSWORD_RESET_REQUIRED';
}

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [trocaObrigatoria, setTrocaObrigatoria] = useState<TrocaSenhaObrigatoriaPendente | null>(null);
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacaoNovaSenha, setConfirmacaoNovaSenha] = useState('');
  const {
    usuario: sessao,
    carregandoSessao,
    concluirTrocaSenhaObrigatoria,
    login,
  } = useAutenticacao();
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();

  const isDarkTheme = theme === 'dark';

  useEffect(() => {
    if (!carregandoSessao && sessao) {
      navigate(firstAccessibleRoute(sessao), { replace: true });
    }
  }, [carregandoSessao, navigate, sessao]);

  async function handleLogin(e: FormEvent) {
    e.preventDefault();
    setErro('');
    setCarregando(true);

    try {
      const resposta = await login({ email, senha });
      navigate(firstAccessibleRoute({
        papel: resposta.usuario.papel,
        permissoesEfetivas: resposta.usuario.permissoesEfetivas,
        exigeTrocaSenha: resposta.exigeTrocaSenha ?? resposta.usuario.exigeTrocaSenha,
      }), { replace: true });
    } catch (error) {
      if (isPasswordResetRequiredError(error)) {
        setTrocaObrigatoria({ email: email.trim(), senhaTemporaria: senha });
        setErro('');
        return;
      }
      setErro(getApiErrorMessage(error, 'Usuário ou senha inválidos.'));
    } finally {
      setCarregando(false);
    }
  }

  async function handleNovaSenhaObrigatoria(e: FormEvent) {
    e.preventDefault();
    if (!trocaObrigatoria) return;

    setErro('');
    if (novaSenha !== confirmacaoNovaSenha) {
      setErro('A confirmação da nova senha não confere.');
      return;
    }

    const passwordErrors = getPasswordPolicyErrors(novaSenha);
    if (passwordErrors.length > 0) {
      setErro(passwordErrors[0]);
      return;
    }

    setCarregando(true);
    try {
      const resposta = await concluirTrocaSenhaObrigatoria({
        email: trocaObrigatoria.email,
        senhaTemporaria: trocaObrigatoria.senhaTemporaria,
        novaSenha,
      });
      setSenha('');
      setNovaSenha('');
      setConfirmacaoNovaSenha('');
      setTrocaObrigatoria(null);
      navigate(firstAccessibleRoute({
        papel: resposta.usuario.papel,
        permissoesEfetivas: resposta.usuario.permissoesEfetivas,
        exigeTrocaSenha: false,
      }), { replace: true });
    } catch (error) {
      setErro(getApiErrorMessage(error, 'Não foi possível definir a nova senha.'));
    } finally {
      setCarregando(false);
    }
  }

  function toggleTheme() {
    setTheme(isDarkTheme ? 'light' : 'dark');
  }

  return (
    <div
      className="flex min-h-screen flex-col transition-colors duration-300"
      style={{ backgroundColor: 'var(--color-bg)' }}
    >
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between p-4 sm:p-6">
        <div className="flex items-center gap-3">
          <img
            src="/logo.png"
            alt="Logo da empresa"
            className="h-7 w-auto object-contain transition-all duration-200 dark:brightness-0 dark:invert"
          />
          <div className="hidden h-4 w-[1px] sm:block" style={{ backgroundColor: 'var(--color-border)' }} />
          <span className="hidden text-[11px] font-bold uppercase tracking-wider sm:block" style={{ color: 'var(--color-text-muted)' }}>
            Portal de Indicadores
          </span>
        </div>

        <button
          type="button"
          onClick={toggleTheme}
          title={isDarkTheme ? 'Alternar para tema claro' : 'Alternar para tema escuro'}
          aria-label={isDarkTheme ? 'Alternar para tema claro' : 'Alternar para tema escuro'}
          className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-xl border shadow-sm transition-colors focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]/20"
          style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
        >
          {isDarkTheme ? <Sun size={16} /> : <Moon size={16} />}
        </button>
      </div>

      <div className="flex flex-1 items-center justify-center px-4 py-6">
        <div className="flex w-full max-w-[780px] flex-col items-stretch gap-5 md:flex-row">
          <form
            onSubmit={trocaObrigatoria ? handleNovaSenhaObrigatoria : handleLogin}
            className="flex flex-1 flex-col gap-5 rounded-[24px] border p-7 shadow-sm transition-all duration-300 hover:shadow-md"
            style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
          >
            <div className="flex flex-col gap-2">
              <div className="space-y-1">
                <h1 className="text-lg font-bold tracking-tight" style={{ color: 'var(--color-text)' }}>
                  {trocaObrigatoria ? 'Defina sua nova senha' : 'Acesso à Plataforma'}
                </h1>
                <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  {trocaObrigatoria
                    ? 'A senha temporária foi validada. Crie uma senha privada para continuar.'
                    : 'Insira suas credenciais corporativas para entrar na sua conta'}
                </p>
              </div>

              {carregandoSessao && (
                <div
                  className="mt-1 self-start rounded-lg px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wider"
                  style={{ backgroundColor: 'var(--color-warning-badge-bg)', color: 'var(--color-warning-badge-text)' }}
                >
                  Validando sessão salva...
                </div>
              )}
            </div>

            {erro && (
              <div
                role="alert"
                className="rounded-xl border px-3.5 py-2.5 text-xs font-medium"
                style={{
                  backgroundColor: 'var(--color-negative-badge-bg)',
                  color: 'var(--color-negative-badge-text)',
                  borderColor: 'var(--color-negative-border)',
                }}
              >
                {erro}
              </div>
            )}

            {trocaObrigatoria ? (
              <>
                <div className="space-y-4">
                  <label className="flex flex-col gap-1.5">
                    <span className="text-[13px] font-semibold" style={{ color: 'var(--color-text-muted)' }}>Nova senha</span>
                    <input
                      type={mostrarSenha ? 'text' : 'password'}
                      value={novaSenha}
                      onChange={(e) => setNovaSenha(e.target.value)}
                      placeholder="••••••••"
                      className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
                      style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                      minLength={12}
                      required
                    />
                    <span className="text-[11px]" style={{ color: 'var(--color-text-muted)' }}>{PASSWORD_POLICY_HINT}</span>
                  </label>
                  <label className="flex flex-col gap-1.5">
                    <span className="text-[13px] font-semibold" style={{ color: 'var(--color-text-muted)' }}>Confirmar nova senha</span>
                    <input
                      type={mostrarSenha ? 'text' : 'password'}
                      value={confirmacaoNovaSenha}
                      onChange={(e) => setConfirmacaoNovaSenha(e.target.value)}
                      placeholder="••••••••"
                      className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
                      style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                      minLength={12}
                      required
                    />
                  </label>
                  <button
                    type="button"
                    onClick={() => setMostrarSenha((prev) => !prev)}
                    className="text-left text-xs font-semibold transition-colors hover:underline"
                    style={{ color: 'var(--color-primary)' }}
                  >
                    {mostrarSenha ? 'Ocultar senhas' : 'Exibir senhas'}
                  </button>
                </div>
                <button
                  type="submit"
                  disabled={carregando}
                  className="w-full cursor-pointer rounded-xl py-3 text-sm font-semibold shadow-sm transition-all duration-150 hover:shadow focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]/20 active:scale-[0.98] disabled:opacity-50"
                  style={{ backgroundColor: 'var(--color-primary)', color: '#ffffff' }}
                >
                  {carregando ? 'Salvando nova senha...' : 'Salvar nova senha e entrar'}
                </button>
              </>
            ) : (
              <>
                <div className="space-y-4">
                  <label className="flex flex-col gap-1.5">
                    <span className="text-[13px] font-semibold" style={{ color: 'var(--color-text-muted)' }}>E-mail</span>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="seu.email@empresa.com"
                      className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
                      style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                      required
                    />
                  </label>

                  <label className="flex flex-col gap-1.5">
                    <span className="text-[13px] font-semibold" style={{ color: 'var(--color-text-muted)' }}>Senha</span>
                    <div className="relative w-full">
                      <input
                        type={mostrarSenha ? 'text' : 'password'}
                        value={senha}
                        onChange={(e) => setSenha(e.target.value)}
                        placeholder="••••••••"
                        className="w-full rounded-xl border py-2.5 pl-3.5 pr-11 text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
                        style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                        required
                      />
                      <button
                        type="button"
                        onClick={() => setMostrarSenha((prev) => !prev)}
                        title={mostrarSenha ? 'Ocultar senha' : 'Exibir senha'}
                        className="absolute right-3.5 top-1/2 -translate-y-1/2 cursor-pointer rounded-md p-1 transition-colors focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]/20"
                        style={{ color: 'var(--color-text-muted)' }}
                      >
                        {mostrarSenha ? <EyeOff size={16} /> : <Eye size={16} />}
                      </button>
                    </div>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={carregando}
                  className="w-full cursor-pointer rounded-xl py-3 text-sm font-semibold shadow-sm transition-all duration-150 hover:shadow focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]/20 active:scale-[0.98] disabled:opacity-50"
                  style={{ backgroundColor: 'var(--color-primary)', color: '#ffffff' }}
                >
                  {carregando ? (
                    <span className="flex items-center justify-center gap-2">
                      <Loader2 size={16} className="animate-spin" />
                      <span>Entrando...</span>
                    </span>
                  ) : (
                    <span>Acessar plataforma</span>
                  )}
                </button>
              </>
            )}

            {carregando && (
              <p className="text-center text-[11px]" style={{ color: 'var(--color-text-muted)' }}>
                {trocaObrigatoria ? 'Protegendo sua nova senha...' : 'Preparando painel e validando credenciais...'}
              </p>
            )}

            <div className="flex items-center justify-center gap-1.5 border-t pt-4 text-[11px]" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>
              <ShieldCheck size={13} style={{ color: 'var(--color-primary)' }} />
              <span>Acesso restrito e criptografado SSL</span>
            </div>
          </form>

          <aside
            className="hidden w-[300px] flex-none flex-col justify-between rounded-[24px] border p-7 shadow-sm transition-all duration-300 hover:shadow-md md:flex"
            style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
          >
            <div className="space-y-4">
              <div
                className="flex h-11 w-11 items-center justify-center rounded-xl"
                style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-primary)' }}
              >
                <BarChart3 size={20} />
              </div>

              <div className="space-y-2">
                <span className="text-[10px] font-bold uppercase tracking-wider" style={{ color: 'var(--color-text-muted)' }}>
                  Portal corporativo
                </span>
                <h2 className="text-xl font-bold tracking-tight" style={{ color: 'var(--color-text)' }}>
                  Indicadores em um só lugar
                </h2>
                <p className="text-sm leading-6" style={{ color: 'var(--color-text-muted)' }}>
                  Acesse os painéis da empresa com segurança e visualize as áreas liberadas para o seu perfil.
                </p>
              </div>
            </div>

            <div className="space-y-2 border-t pt-5" style={{ borderColor: 'var(--color-border)' }}>
              <div className="flex items-center gap-2 text-xs font-semibold" style={{ color: 'var(--color-text)' }}>
                <ShieldCheck size={15} style={{ color: 'var(--color-primary)' }} />
                <span>Acesso seguro por usuário</span>
              </div>
              <p className="text-[11px] leading-5" style={{ color: 'var(--color-text-muted)' }}>
                As permissões são aplicadas automaticamente após o login.
              </p>
            </div>
          </aside>
        </div>
      </div>

      <footer
        className="mt-auto w-full py-6 text-center text-[11px]"
        style={{ color: 'var(--color-text-muted)' }}
      >
        Desenvolvido por{' '}
        <a
          href="https://www.linkedin.com/in/dev-lucasandrade/"
          target="_blank"
          rel="noopener noreferrer"
          className="font-medium hover:underline"
          style={{ color: 'var(--color-primary)' }}
        >
          @valentelucass
        </a>
        {' '} • Suporte: lucasmac.dev@gmail.com
      </footer>
    </div>
  );
}
