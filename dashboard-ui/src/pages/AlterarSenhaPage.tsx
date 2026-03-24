import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import { firstAccessibleRoute } from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';

export default function AlterarSenhaPage() {
  const { usuario, alterarSenha, logout } = useAutenticacao();
  const navigate = useNavigate();

  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacaoSenha, setConfirmacaoSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErro('');

    if (novaSenha !== confirmacaoSenha) {
      setErro('A confirmação da nova senha não confere.');
      return;
    }

    setCarregando(true);

    try {
      await alterarSenha({ senhaAtual, novaSenha });
      if (usuario) {
        navigate(firstAccessibleRoute({ ...usuario, exigeTrocaSenha: false }), { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } catch (error) {
      setErro(getApiErrorMessage(error, 'Não foi possível alterar a senha.'));
    } finally {
      setCarregando(false);
    }
  }

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 px-4">
      <form onSubmit={handleSubmit} className="flex w-full max-w-md flex-col gap-4 rounded-3xl border border-gray-200 bg-white p-8 shadow-sm">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Alterar senha</h1>
          <p className="mt-1 text-sm text-gray-500">
            {usuario?.exigeTrocaSenha
              ? 'Você precisa definir uma nova senha antes de continuar.'
              : 'Atualize sua senha para continuar usando a plataforma.'}
          </p>
        </div>

        {erro && <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

        <label className="space-y-1">
          <span className="text-sm font-medium text-gray-700">Senha atual</span>
          <input
            type="password"
            value={senhaAtual}
            onChange={(event) => setSenhaAtual(event.target.value)}
            className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
            required
          />
        </label>

        <label className="space-y-1">
          <span className="text-sm font-medium text-gray-700">Nova senha</span>
          <input
            type="password"
            value={novaSenha}
            onChange={(event) => setNovaSenha(event.target.value)}
            className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
            minLength={8}
            required
          />
        </label>

        <label className="space-y-1">
          <span className="text-sm font-medium text-gray-700">Confirmar nova senha</span>
          <input
            type="password"
            value={confirmacaoSenha}
            onChange={(event) => setConfirmacaoSenha(event.target.value)}
            className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
            minLength={8}
            required
          />
        </label>

        <div className="flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={carregando}
            className="rounded-xl bg-[#21478A] px-4 py-2.5 font-medium text-white disabled:opacity-50"
          >
            {carregando ? 'Salvando...' : 'Salvar nova senha'}
          </button>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-xl border border-gray-300 px-4 py-2.5 font-medium text-gray-700"
          >
            Sair
          </button>
        </div>
      </form>
    </div>
  );
}
