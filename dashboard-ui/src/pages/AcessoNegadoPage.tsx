import { useNavigate } from 'react-router-dom';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import { firstAccessibleRoute } from '../utils/accessControl';

export default function AcessoNegadoPage() {
  const { usuario, logout, carregandoSessao } = useAutenticacao();
  const navigate = useNavigate();
  const destinoPadrao = firstAccessibleRoute(usuario);

  function handleSair() {
    logout();
    navigate('/login', { replace: true });
  }

  function handleIrParaDashboard() {
    navigate(destinoPadrao, { replace: true });
  }

  if (carregandoSessao) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-100">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#21478A] border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-lg rounded-3xl border border-gray-200 bg-white p-8 text-center shadow-sm">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-2xl font-bold text-red-600">
          !
        </div>
        <h1 className="mt-5 text-2xl font-bold text-gray-900">Acesso negado</h1>
        <p className="mt-2 text-sm text-gray-600">
          Voce nao tem permissao para acessar este recurso com o perfil atual.
        </p>

        <div className="mt-6 flex flex-wrap justify-center gap-3">
          {usuario?.token && destinoPadrao !== '/acesso-negado' ? (
            <>
              <button
                type="button"
                onClick={handleIrParaDashboard}
                className="rounded-xl bg-[#21478A] px-4 py-2.5 text-sm font-medium text-white"
              >
                Ir para meu dashboard
              </button>
              <button
                type="button"
                onClick={handleSair}
                className="rounded-xl border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700"
              >
                Trocar usuario
              </button>
            </>
          ) : usuario?.token ? (
            <button
              type="button"
              onClick={handleSair}
              className="rounded-xl bg-[#21478A] px-4 py-2.5 text-sm font-medium text-white"
            >
              Trocar usuario
            </button>
          ) : (
            <button
              type="button"
              onClick={() => navigate('/login', { replace: true })}
              className="rounded-xl bg-[#21478A] px-4 py-2.5 text-sm font-medium text-white"
            >
              Ir para login
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
