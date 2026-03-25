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
export type OverrideTipo = 'DENY' | 'GRANT';
export type PermissionOverrideMode = 'inherit' | 'deny' | 'grant';
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
  templatePermissoes: PermissionMap;
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
  nome: string;
  email: string;
  ativo: boolean;
  setorId: string;
  setorNome: string;
  papel: string;
  permissoesEfetivas: PermissionMap;
  filiaisPermitidasEfetivas: string[];
  permissoesNegadas: PermissionKey[];
  permissoesConcedidas: PermissionKey[];
}

export interface UsuarioPayload {
  nome: string;
  email: string;
  senha?: string;
  confirmacaoSenha?: string;
  setorId: string;
  papel: string;
  permissoesNegadas: PermissionKey[];
  permissoesConcedidas: PermissionKey[];
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
