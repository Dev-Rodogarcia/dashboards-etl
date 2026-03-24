import { useMemo } from 'react';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import {
  canAccess,
  createEmptyPermissionMap,
  hasRole,
  isAdminAcesso,
  isAdminPlataforma,
} from '../utils/accessControl';
import type { PermissionKey } from '../types/access';

export function usePermissions() {
  const { usuario } = useAutenticacao();

  return useMemo(() => ({
    isAdminAcesso: isAdminAcesso(usuario),
    isAdminPlataforma: isAdminPlataforma(usuario),
    permissions: usuario?.setor.permissoes ?? createEmptyPermissionMap(),
    canAccess: (permission: PermissionKey) => canAccess(usuario, permission),
    hasRole: (role: string) => hasRole(usuario, role),
    setorNome: usuario?.setor.nome ?? '',
  }), [usuario]);
}
