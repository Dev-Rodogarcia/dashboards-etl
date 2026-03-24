import type { ReactNode } from 'react';
import { usePermissions } from '../hooks/usePermissions';
import type { PermissionKey } from '../types/access';

interface PermissionGateProps {
  children: ReactNode;
  permission?: PermissionKey;
  adminOnly?: boolean;
  role?: string;
  fallback?: ReactNode;
}

export default function PermissionGate({ children, permission, adminOnly, role, fallback = null }: PermissionGateProps) {
  const { isAdminAcesso, canAccess, hasRole } = usePermissions();

  if (adminOnly && !isAdminAcesso) {
    return <>{fallback}</>;
  }

  if (role && !hasRole(role)) {
    return <>{fallback}</>;
  }

  if (permission && !canAccess(permission)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
