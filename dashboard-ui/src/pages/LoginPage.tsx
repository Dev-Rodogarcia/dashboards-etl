import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import { firstAccessibleRoute } from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const { usuario: sessao, carregandoSessao, login } = useAutenticacao();
  const navigate = useNavigate();

  useEffect(() => {
    if (!carregandoSessao && sessao?.token) {
      navigate(firstAccessibleRoute(sessao), { replace: true });
    }
  }, [carregandoSessao, navigate, sessao]);

  async function handleSubmit(e: FormEvent) {
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
      setErro(getApiErrorMessage(error, 'Usuário ou senha inválidos.'));
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 px-4">
      <form onSubmit={handleSubmit} className="flex w-full max-w-md flex-col gap-4 rounded-3xl border border-gray-200 bg-white p-8 shadow-sm">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Acesso à plataforma</h1>
          <p className="mt-1 text-sm text-gray-500">
            Controle de acesso por setor com área administrativa.
          </p>
        </div>

        {erro && <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

        <label className="space-y-1">
          <span className="text-sm font-medium text-gray-700">E-mail</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
            required
          />
        </label>

        <label className="space-y-1">
          <span className="text-sm font-medium text-gray-700">Senha</span>
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
            required
          />
        </label>

        <button
          type="submit"
          disabled={carregando}
          className="rounded-xl bg-[#21478A] py-2.5 font-medium text-white disabled:opacity-50"
        >
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
