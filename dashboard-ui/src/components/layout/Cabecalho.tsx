import { useNavigate } from 'react-router-dom';
import { useAutenticacao } from '../../contexts/AutenticacaoContext';

export default function Cabecalho() {
  const { usuario, logout } = useAutenticacao();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6">
      <div>
        <div className="text-xs uppercase tracking-wide text-gray-400">Perfil ativo</div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold text-gray-900">{usuario?.setor.nome}</span>
          {usuario?.admin && (
            <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-700">
              Admin
            </span>
          )}
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="text-right">
          <div className="text-sm font-medium text-gray-800">{usuario?.nome}</div>
          <div className="text-xs text-gray-500">{usuario?.email}</div>
        </div>
        <button
          onClick={handleLogout}
          className="rounded-lg border border-red-200 px-3 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50"
        >
          Sair
        </button>
      </div>
    </header>
  );
}
