import { Navigate, Outlet } from 'react-router-dom';
import { useAutenticacao } from '../../contexts/AutenticacaoContext';
import { usePermissions } from '../../hooks/usePermissions';
import type { PermissionKey } from '../../types/access';

interface RotaProtegidaProps {
  permissao?: PermissionKey;
  adminOnly?: boolean;
  role?: string;
  allowPasswordChange?: boolean;
}

export default function RotaProtegida({ permissao, adminOnly, role, allowPasswordChange }: RotaProtegidaProps) {
  const { usuario, carregandoSessao } = useAutenticacao();
  const { canAccess, hasRole, isAdminAcesso } = usePermissions();

  if (carregandoSessao) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-100">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#21478A] border-t-transparent" />
      </div>
    );
  }

  if (!usuario?.token) {
    return <Navigate to="/login" replace />;
  }

  if (usuario.exigeTrocaSenha && !allowPasswordChange) {
    return <Navigate to="/alterar-senha" replace />;
  }

  if (adminOnly && !isAdminAcesso) {
    return <Navigate to="/acesso-negado" replace />;
  }

  if (role && !hasRole(role)) {
    return <Navigate to="/acesso-negado" replace />;
  }

  if (permissao && !canAccess(permissao)) {
    return <Navigate to="/acesso-negado" replace />;
  }

  return <Outlet />;
}
