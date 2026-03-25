import { NavLink, useNavigate } from 'react-router-dom';
import { useTheme } from 'next-themes';
import { ShieldCheck, ChevronDown } from 'lucide-react';
import { useAutenticacao } from '../../contexts/AutenticacaoContext';
import { usePermissions } from '../../hooks/usePermissions';
import { ADMIN_NAV_ITEMS, DASHBOARD_NAV_ITEMS } from '../../utils/accessControl';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from '../ui/dropdown-menu';

function SunIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="5" />
      <line x1="12" y1="1" x2="12" y2="3" />
      <line x1="12" y1="21" x2="12" y2="23" />
      <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
      <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
      <line x1="1" y1="12" x2="3" y2="12" />
      <line x1="21" y1="12" x2="23" y2="12" />
      <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
      <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>
  );
}


export default function TopNav() {
  const { usuario, logout } = useAutenticacao();
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();
  const { canAccess, isAdminAcesso, isAdminPlataforma } = usePermissions();

  const dashboardsVisiveis = DASHBOARD_NAV_ITEMS.filter((item) =>
    item.permission ? canAccess(item.permission) : true,
  );

  const adminBadge = isAdminPlataforma
    ? 'Admin Plataforma'
    : isAdminAcesso
      ? 'Admin Acesso'
      : null;

  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  function toggleTheme() {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  }

  return (
    <header
      className="sticky top-0 z-50 flex h-14 items-center gap-4 border-b px-5 shadow-sm"
      style={{
        backgroundColor: 'var(--color-card)',
        borderColor: 'var(--color-border)',
      }}
    >
      {/* Logo */}
      <div className="flex shrink-0 items-center pr-4" style={{ borderRight: '1px solid var(--color-border)' }}>
        <img
          src="/logo.png"
          alt="Logo da empresa"
          className="h-7 w-auto object-contain transition-all duration-200 dark:brightness-0 dark:invert"
        />
      </div>

      {/* Navigation links */}
      <nav className="flex flex-1 items-center gap-1 overflow-x-auto">
        {dashboardsVisiveis.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors duration-150 ${
                isActive
                  ? 'text-white'
                  : 'hover:bg-[var(--color-bg)]'
              }`
            }
            style={({ isActive }) =>
              isActive
                ? { backgroundColor: 'var(--color-primary)' }
                : { color: 'var(--color-text-muted)' }
            }
          >
            {item.label}
          </NavLink>
        ))}

        {/* Admin dropdown */}
        {isAdminAcesso && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                className="flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors duration-150 hover:bg-[var(--color-bg)]"
                style={{ color: 'var(--color-text-muted)' }}
              >
                <ShieldCheck size={14} />
                Admin
                <ChevronDown size={12} />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start">
              {ADMIN_NAV_ITEMS.map((item) => (
                <DropdownMenuItem key={item.path} asChild>
                  <NavLink
                    to={item.path}
                    className={({ isActive }) =>
                      `flex w-full flex-col ${isActive ? 'font-semibold' : ''}`
                    }
                    style={({ isActive }) => ({
                      color: isActive ? 'var(--color-primary)' : 'var(--color-text)',
                    })}
                  >
                    <span className="font-medium">{item.label}</span>
                    {item.description && (
                      <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                        {item.description}
                      </span>
                    )}
                  </NavLink>
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </nav>

      {/* Right side: sector + user + dark toggle + logout */}
      <div className="flex shrink-0 items-center gap-3">
        {/* Sector badge */}
        <div className="hidden items-center gap-2 sm:flex">
          <span className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>
            {usuario?.setor.nome}
          </span>
          {adminBadge && (
            <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-700">
              {adminBadge}
            </span>
          )}
        </div>

        <div className="h-5 w-px" style={{ backgroundColor: 'var(--color-border)' }} />

        {/* Dark mode toggle */}
        <button
          onClick={toggleTheme}
          title={theme === 'dark' ? 'Modo claro' : 'Modo escuro'}
          className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors duration-150 hover:bg-[var(--color-bg)]"
          style={{ color: 'var(--color-text-muted)' }}
        >
          {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
        </button>

        {/* Logout */}
        <button
          onClick={() => void handleLogout()}
          className="rounded-lg px-3 py-1.5 text-sm font-medium text-red-500 transition-all duration-150 hover:bg-red-500 hover:text-white"
        >
          Sair
        </button>
      </div>
    </header>
  );
}
