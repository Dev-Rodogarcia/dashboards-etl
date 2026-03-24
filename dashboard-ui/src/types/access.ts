export const PERMISSION_KEYS = [
  'coletas',
  'manifestos',
  'fretes',
  'tracking',
  'faturas',
  'faturasPorCliente',
  'contasAPagar',
  'cotacoes',
  'executivo',
  'etlSaude',
  'dimensoes',
] as const;

export type PermissionKey = (typeof PERMISSION_KEYS)[number];

export type PermissionMap = Record<PermissionKey, boolean>;
export type OverrideTipo = 'GRANT' | 'DENY';
export type PermissionOverrideMode = 'inherit' | 'grant' | 'deny';
export type PermissionOverrideStateMap = Record<PermissionKey, PermissionOverrideMode>;

export interface PermissionCatalogItem {
  chave: PermissionKey;
  nome: string;
  descricao: string;
  rota: string | null;
}

export interface SetorAdmin {
  id: string;
  nome: string;
  descricao: string | null;
  sistema: boolean;
  totalUsuarios: number;
  permissoes: PermissionMap;
  filiaisPermitidas: string[];
}

export interface SetorPayload {
  nome: string;
  descricao: string | null;
  permissoes: PermissionMap;
  filiaisPermitidas: string[];
}

export interface UsuarioAdmin {
  id: string;
  login: string;
  nome: string;
  email: string;
  admin: boolean;
  ativo: boolean;
  setorId: string;
  setorNome: string;
  permissoes: PermissionMap;
  papeis: string[];
}

export interface UsuarioPayload {
  login: string;
  nome: string;
  email: string;
  senha?: string;
  setorId: string;
  admin: boolean;
  ativo: boolean;
}

export interface PapelAdmin {
  id: number;
  nome: string;
  descricao: string | null;
  nivel: number;
}

export interface PermissaoOverride {
  permissaoChave: PermissionKey;
  tipo: OverrideTipo;
}

export interface AuditLogEntry {
  id: number;
  timestamp: string;
  usuarioLogin: string | null;
  acao: string;
  recurso: string | null;
  detalhesJson: string | null;
  ipAddress: string | null;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalPages: number;
  totalElements: number;
}
