import { useMemo } from 'react';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import {
  canHardDeleteUsers,
  canAccess,
  createEmptyPermissionMap,
  hasRole,
  isAdminAcesso,
  isDesenvolvedor,
  isAdminPlataforma,
} from '../utils/accessControl';
import type { PermissionKey } from '../types/access';

export function usePermissions() {
  const { usuario } = useAutenticacao();

  return useMemo(() => ({
    canHardDeleteUsers: canHardDeleteUsers(usuario),
    isDesenvolvedor: isDesenvolvedor(usuario),
    isAdminAcesso: isAdminAcesso(usuario),
    isAdminPlataforma: isAdminPlataforma(usuario),
    permissions: usuario?.permissoesEfetivas ?? createEmptyPermissionMap(),
    canAccess: (permission: PermissionKey) => canAccess(usuario, permission),
    hasRole: (role: string) => hasRole(usuario, role),
    setorNome: usuario?.setor.nome ?? '',
  }), [usuario]);
}
