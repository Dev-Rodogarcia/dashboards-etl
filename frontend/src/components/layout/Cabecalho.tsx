import { useNavigate } from 'react-router-dom';
import { useAutenticacao } from '../../contexts/AutenticacaoContext';

export default function Cabecalho() {
  const { usuario, logout } = useAutenticacao();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <header
      className="flex h-16 items-center justify-between border-b px-6"
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
    >
      <div>
        <div className="text-xs uppercase tracking-wide" style={{ color: 'var(--color-text-muted)' }}>
          Perfil ativo
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{usuario?.setor.nome}</span>
          {usuario?.papel === 'admin_plataforma' && (
            <span
              className="rounded-full px-2 py-0.5 text-[11px] font-semibold"
              style={{ backgroundColor: 'rgba(249, 115, 22, 0.16)', color: '#ea580c' }}
            >
              Admin
            </span>
          )}
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="text-right">
          <div className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>{usuario?.nome}</div>
          <div className="text-xs" style={{ color: 'var(--color-text-muted)' }}>{usuario?.email}</div>
        </div>
        <button
          onClick={() => void handleLogout()}
          className="rounded-lg border px-3 py-2 text-sm font-medium transition-colors hover:bg-[rgba(220,38,38,0.12)]"
          style={{ borderColor: 'rgba(220, 38, 38, 0.45)', color: '#dc2626' }}
        >
          Sair
        </button>
      </div>
    </header>
  );
}
