import type { IUsuarioSessao } from '../types/auth';
import type {
  PermissionCatalogItem,
  PermissionKey,
  PermissionMap,
  PermissionOverrideStateMap,
} from '../types/access';

export const PAPEL_ADMIN_PLATAFORMA = 'admin_plataforma';
export const PAPEL_ADMIN_ACESSO = 'admin_acesso';

export interface NavItem {
  label: string;
  path: string;
  permission?: PermissionKey;
  adminOnly?: boolean;
  description?: string;
}

export const DASHBOARD_NAV_ITEMS: NavItem[] = [
  { label: 'Coletas', path: '/coletas', permission: 'coletas' },
  { label: 'Manifestos', path: '/manifestos', permission: 'manifestos' },
  { label: 'Fretes', path: '/fretes', permission: 'fretes' },
  { label: 'Localização de Cargas', path: '/tracking', permission: 'tracking' },
  { label: 'Faturas', path: '/faturas', permission: 'faturas' },
  { label: 'Faturas por Cliente', path: '/faturas-por-cliente', permission: 'faturasPorCliente' },
  { label: 'Contas a Pagar', path: '/contas-a-pagar', permission: 'contasAPagar' },
  { label: 'Cotações', path: '/cotacoes', permission: 'cotacoes' },
  { label: 'Executivo', path: '/executivo', permission: 'executivo' },
  { label: 'ETL Saúde', path: '/etl-saude', permission: 'etlSaude' },
];

export const ADMIN_NAV_ITEMS: NavItem[] = [
  { label: 'Setores', path: '/admin/setores', adminOnly: true, description: 'Perfis e permissões' },
  { label: 'Usuários', path: '/admin/usuarios', adminOnly: true, description: 'Atribuição de setores' },
];

export function createEmptyPermissionMap(): PermissionMap {
  return {
    coletas: false,
    manifestos: false,
    fretes: false,
    tracking: false,
    faturas: false,
    faturasPorCliente: false,
    contasAPagar: false,
    cotacoes: false,
    executivo: false,
    etlSaude: false,
    dimensoes: false,
  };
}

export function normalizePermissionMap(raw?: Partial<Record<PermissionKey, boolean>>): PermissionMap {
  return {
    ...createEmptyPermissionMap(),
    ...(raw ?? {}),
  };
}

export function createEmptyPermissionOverrideState(): PermissionOverrideStateMap {
  return {
    coletas: 'inherit',
    manifestos: 'inherit',
    fretes: 'inherit',
    tracking: 'inherit',
    faturas: 'inherit',
    faturasPorCliente: 'inherit',
    contasAPagar: 'inherit',
    cotacoes: 'inherit',
    executivo: 'inherit',
    etlSaude: 'inherit',
    dimensoes: 'inherit',
  };
}

export function hasRole(user: Pick<IUsuarioSessao, 'papeis'> | null, role: string): boolean {
  return Boolean(user?.papeis.includes(role));
}

export function isAdminPlataforma(user: Pick<IUsuarioSessao, 'papeis'> | null): boolean {
  return hasRole(user, PAPEL_ADMIN_PLATAFORMA);
}

export function isAdminAcesso(user: Pick<IUsuarioSessao, 'papeis'> | null): boolean {
  return isAdminPlataforma(user) || hasRole(user, PAPEL_ADMIN_ACESSO);
}

export function canAccess(user: IUsuarioSessao | null, permission?: PermissionKey): boolean {
  if (!user) return false;
  if (isAdminPlataforma(user)) return true;
  if (!permission) return true;
  return Boolean(user.setor.permissoes[permission]);
}

export function firstAccessibleRoute(user: Pick<IUsuarioSessao, 'setor' | 'papeis' | 'exigeTrocaSenha'> | null): string {
  if (!user) return '/login';
  if (user.exigeTrocaSenha) return '/alterar-senha';

  const match = DASHBOARD_NAV_ITEMS.find((item) =>
    item.permission ? isAdminPlataforma(user) || user.setor.permissoes[item.permission] : false,
  );

  if (match) {
    return match.path;
  }

  return isAdminAcesso(user) ? '/admin/usuarios' : '/acesso-negado';
}

export function buildPermissionMapFromCatalog(catalog: PermissionCatalogItem[]): PermissionMap {
  return catalog.reduce((acc, item) => {
    acc[item.chave] = false;
    return acc;
  }, createEmptyPermissionMap());
}
