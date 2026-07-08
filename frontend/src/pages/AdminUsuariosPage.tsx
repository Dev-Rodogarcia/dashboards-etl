import { useEffect, useId, useMemo, useState } from 'react';
import type { FocusEvent, FormEvent, ReactNode } from 'react';
import { Eye, MoreHorizontal, Pencil, Upload, UserX } from 'lucide-react';
import FiliaisPermitidasSplitSelect from '../components/admin/FiliaisPermitidasSplitSelect';
import PermissionOverrideMatrix from '../components/admin/PermissionOverrideMatrix';
import UsuariosImportacaoModal from '../components/admin/UsuariosImportacaoModal';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import { usePageHeader } from '../contexts/PageHeaderContext';
import {
  useAtualizarUsuario,
  useCatalogoPermissoes,
  useCriarUsuario,
  useExcluirUsuario,
  usePapeisAdmin,
  useResumoSessoesUsuariosAdmin,
  useSetoresAdmin,
  useUsuariosAdmin,
} from '../hooks/queries/useAdminAcesso';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { usePermissions } from '../hooks/usePermissions';
import type {
  PapelAdmin,
  PermissionKey,
  PermissionMap,
  PermissionOverrideStateMap,
  UsuarioAdmin,
  UsuarioPayload,
} from '../types/access';
import {
  buildPermissionMapFromCatalog,
  createEmptyPermissionMap,
  createEmptyPermissionOverrideState,
  PAPEL_ADMIN_PLATAFORMA,
  PAPEL_DESENVOLVEDOR,
  permissionSummary,
} from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';
import { formatarDataHoraMinuto } from '../utils/formatadores';
import { getPasswordPolicyErrors, PASSWORD_POLICY_HINT } from '../utils/passwordPolicy';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { Popover, PopoverContent, PopoverTrigger } from '../components/ui/popover';

interface UsuarioRow extends UsuarioAdmin {
  acoes: string;
  detalhes: string;
  papelResumo: string;
  permissoesResumo: string;
  negacoesResumo: string;
  concessoesResumo: string;
  filiaisResumo: string;
  senhaResumo: string;
}

const FORM_INICIAL: UsuarioPayload = {
  nome: '',
  email: '',
  senha: '',
  confirmacaoSenha: '',
  setorId: '',
  papel: 'usuario_comum',
  permissoesNegadas: [],
  permissoesConcedidas: [],
  escopoFiliaisTipo: 'HERDAR_SETOR',
  filiaisPermitidasUsuario: [],
  ativo: true,
};

const ESCOPO_FILIAIS_OPTIONS: Array<{
  value: UsuarioPayload['escopoFiliaisTipo'];
  label: string;
  description: string;
}> = [
  { value: 'HERDAR_SETOR', label: 'Herdar do setor', description: 'Usa as filiais configuradas no setor selecionado.' },
  { value: 'TODAS', label: 'Todas as filiais', description: 'Libera dados de todas as filiais.' },
  { value: 'SELECIONADAS', label: 'Somente selecionadas', description: 'Usa uma lista específica para este usuário.' },
];

const SURFACE_STYLE = {
  backgroundColor: 'var(--color-card)',
  borderColor: 'var(--color-border)',
};

const FIELD_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const SOFT_PANEL_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
};

const WARNING_PANEL_STYLE = {
  backgroundColor: 'rgba(249, 115, 22, 0.12)',
  borderColor: 'rgba(249, 115, 22, 0.45)',
  color: 'var(--color-text)',
};

const SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const EDIT_DANGER_STYLE = {
  borderColor: '#dc2626',
  color: '#dc2626',
};

const FOCUS_RING_CLASS = 'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-card)]';

const ACTIVE_BADGE_STYLE = {
  backgroundColor: 'rgba(22, 163, 74, 0.14)',
  color: '#15803d',
};

const INACTIVE_BADGE_STYLE = {
  backgroundColor: 'rgba(220, 38, 38, 0.14)',
  color: '#dc2626',
};

const ONLINE_BADGE_STYLE = {
  backgroundColor: 'rgba(22, 163, 74, 0.14)',
  color: '#15803d',
};

const OFFLINE_BADGE_STYLE = {
  backgroundColor: 'rgba(100, 116, 139, 0.14)',
  color: 'var(--color-text-muted)',
};

const PASSWORD_STATUS_STYLE = {
  segura: {
    backgroundColor: 'rgba(22, 163, 74, 0.14)',
    color: '#15803d',
  },
  migrar_no_login: {
    backgroundColor: 'rgba(249, 115, 22, 0.16)',
    color: '#ea580c',
  },
  reset_obrigatorio: {
    backgroundColor: 'rgba(220, 38, 38, 0.14)',
    color: '#dc2626',
  },
} as const;

function formatPasswordStatus(status: UsuarioAdmin['statusSenha']): string {
  switch (status) {
    case 'segura':
      return 'Segura';
    case 'migrar_no_login':
      return 'Migrar no login';
    case 'reset_obrigatorio':
      return 'Reset obrigatório';
    default:
      return status;
  }
}

function renderPasswordStatusBadge(status: UsuarioAdmin['statusSenha'], algoritmo: string) {
  const style = PASSWORD_STATUS_STYLE[status];

  return (
    <div className="flex flex-col gap-1">
      <span className="inline-flex w-fit rounded-full px-2 py-1 text-xs font-medium" style={style}>
        {formatPasswordStatus(status)}
      </span>
      <span className="text-[11px]" style={{ color: 'var(--color-text-subtle)' }}>
        Hash: {algoritmo}
      </span>
    </div>
  );
}

interface SummaryCardProps {
  label: string;
  value: number;
  accent: string;
  pulse?: boolean;
  tooltip?: ReactNode;
}

function SummaryCard({ label, value, accent, pulse, tooltip }: SummaryCardProps) {
  const [isHovered, setIsHovered] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const tooltipId = useId();
  const open = Boolean(tooltip) && (isHovered || isFocused);

  function handleBlur(event: FocusEvent<HTMLDivElement>) {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setIsFocused(false);
    }
  }

  const card = (
    <div className="rounded-lg border px-4 py-3" style={SURFACE_STYLE}>
      <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
        {label}
      </div>
      <div className="mt-2 flex items-center gap-2">
        {pulse && <span className="h-2.5 w-2.5 rounded-full bg-[#10b981] motion-safe:animate-pulse" aria-hidden="true" />}
        <div className="text-2xl font-bold" style={{ color: accent }}>
          {value}
        </div>
      </div>
    </div>
  );

  if (!tooltip) {
    return card;
  }

  return (
    <Popover
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) {
          setIsHovered(false);
          setIsFocused(false);
        }
      }}
    >
      <PopoverTrigger asChild>
        <div
          aria-describedby={open ? tooltipId : undefined}
          onBlur={handleBlur}
          onFocus={() => setIsFocused(true)}
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              setIsHovered(false);
              setIsFocused(false);
            }
          }}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
          tabIndex={0}
          className={`rounded-lg ${FOCUS_RING_CLASS}`}
        >
          {card}
        </div>
      </PopoverTrigger>
      <PopoverContent
        id={tooltipId}
        role="tooltip"
        side="top"
        align="center"
        sideOffset={8}
        collisionPadding={12}
        onOpenAutoFocus={(event) => event.preventDefault()}
        className="pointer-events-none p-4"
        style={{
          width: 'min(24rem, calc(100vw - 1.5rem))',
          color: 'var(--color-text)',
        }}
      >
        {tooltip}
      </PopoverContent>
    </Popover>
  );
}

function timestampAtividade(valor: string | null): number {
  if (!valor) return 0;

  const data = new Date(valor);
  const timestamp = data.getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatTempoUltimoPulso(valor: string | null): string {
  const timestamp = timestampAtividade(valor);
  if (!timestamp) return 'Sem pulso registrado';

  const segundos = Math.max(0, Math.floor((Date.now() - timestamp) / 1000));
  if (segundos < 10) return 'Ativo agora';
  if (segundos < 60) return `Ativo há ${segundos}s`;

  const minutos = Math.floor(segundos / 60);
  if (minutos < 60) return `Ativo há ${minutos} min`;

  const horas = Math.floor(minutos / 60);
  const minutosRestantes = minutos % 60;
  return `Ativo há ${horas}h ${minutosRestantes}min`;
}

function UsuariosOnlineTooltip({ usuarios, isLoading }: { usuarios: UsuarioRow[]; isLoading: boolean }) {
  if (isLoading) {
    return (
      <p className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
        Carregando usuários online...
      </p>
    );
  }

  if (usuarios.length === 0) {
    return (
      <p className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
        Nenhum usuário online agora.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-sm font-bold" style={{ color: 'var(--color-text)' }}>
        Usuários online agora
      </p>
      <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
        {usuarios.map((usuario) => (
          <div key={usuario.id} className="rounded-xl border px-3 py-2" style={SOFT_PANEL_STYLE}>
            <p className="truncate text-sm font-semibold" title={usuario.nome} style={{ color: 'var(--color-text)' }}>
              {usuario.nome}
            </p>
            <p className="mt-0.5 truncate text-xs" title={formatUltimaAtividade(usuario.ultimaAtividade)} style={{ color: 'var(--color-text-subtle)' }}>
              {formatTempoUltimoPulso(usuario.ultimaAtividade)}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}

function formatRoleName(nome: string): string {
  return nome
    .split('_')
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(' ');
}

function formatFiliaisResumo(filiais: string[], acessoTotal: boolean): string {
  if (acessoTotal) return 'Acesso total';
  return filiais.join(', ') || 'Nenhuma';
}

function mapOverridesToState(permissoesNegadas: string[], permissoesConcedidas: string[]): PermissionOverrideStateMap {
  const proximo = createEmptyPermissionOverrideState();

  for (const permissao of permissoesNegadas) {
    proximo[permissao as keyof PermissionOverrideStateMap] = 'deny';
  }
  for (const permissao of permissoesConcedidas) {
    proximo[permissao as keyof PermissionOverrideStateMap] = 'grant';
  }

  return proximo;
}

function mapStateToNegacoes(state: PermissionOverrideStateMap): UsuarioPayload['permissoesNegadas'] {
  return Object.entries(state)
    .filter(([, valor]) => valor === 'deny')
    .map(([permissaoChave]) => permissaoChave as UsuarioPayload['permissoesNegadas'][number]);
}

function mapStateToConcedidas(state: PermissionOverrideStateMap): UsuarioPayload['permissoesConcedidas'] {
  return Object.entries(state)
    .filter(([, valor]) => valor === 'grant')
    .map(([permissaoChave]) => permissaoChave as UsuarioPayload['permissoesConcedidas'][number]);
}

function buildUsuarioPayload(usuario: UsuarioAdmin, permissoesNegadas: PermissionKey[], permissoesConcedidas: PermissionKey[]): UsuarioPayload {
  return {
    nome: usuario.nome,
    email: usuario.email,
    setorId: usuario.setorId,
    papel: usuario.papel,
    permissoesNegadas,
    permissoesConcedidas,
    escopoFiliaisTipo: usuario.escopoFiliaisTipo,
    filiaisPermitidasUsuario: [...usuario.filiaisPermitidasUsuario],
    ativo: usuario.ativo,
  };
}

function useIsMobileUsersTable() {
  const [isMobile, setIsMobile] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(max-width: 860px)').matches : false,
  );

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    const mediaQuery = window.matchMedia('(max-width: 860px)');

    function handleChange(event: MediaQueryListEvent) {
      setIsMobile(event.matches);
    }

    mediaQuery.addEventListener('change', handleChange);
    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, []);

  return isMobile;
}

function renderStatusBadge(ativo: boolean) {
  return (
    <span
      className="inline-flex w-fit rounded-full px-2 py-0.5 text-xs font-medium"
      style={ativo ? ACTIVE_BADGE_STYLE : INACTIVE_BADGE_STYLE}
    >
      {ativo ? 'Ativo' : 'Inativo'}
    </span>
  );
}

function renderOnlineBadge(isOnline: boolean) {
  return (
    <span
      className="inline-flex w-fit items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
      style={isOnline ? ONLINE_BADGE_STYLE : OFFLINE_BADGE_STYLE}
    >
      {isOnline && <span className="h-1.5 w-1.5 rounded-full bg-[#10b981] motion-safe:animate-pulse" aria-hidden="true" />}
      {isOnline ? 'Online' : 'Offline'}
    </span>
  );
}

function formatUltimaAtividade(valor: string | null): string {
  return valor ? formatarDataHoraMinuto(valor) : 'Sem atividade registrada';
}

function renderMobileUsuarioCell(row: UsuarioRow) {
  return (
    <div className="min-w-[11rem] whitespace-normal">
      <p className="text-sm font-semibold leading-tight" style={{ color: 'var(--color-text)' }}>
        {row.nome}
      </p>
      <p className="mt-1 break-all text-xs leading-relaxed" style={{ color: 'var(--color-text-muted)' }}>
        {row.email}
      </p>
    </div>
  );
}

function renderMobileAccessCell(row: UsuarioRow) {
  return (
    <div className="min-w-[14rem] space-y-2 whitespace-normal text-xs leading-relaxed">
      <div className="flex flex-wrap items-center gap-2">
        {renderStatusBadge(row.ativo)}
        <span className="font-medium" style={{ color: 'var(--color-text)' }}>
          {row.setorNome}
        </span>
      </div>

      <div className="space-y-1 break-words">
        <p style={{ color: 'var(--color-text)' }}>{row.papelResumo || 'Sem papel'}</p>
        <p style={{ color: 'var(--color-text-muted)' }}>{row.senhaResumo}</p>
        <p style={{ color: 'var(--color-text-muted)' }}>
          Última tela: {row.ultimaRotaAcessada ?? 'Sem registro'}
        </p>
      </div>
    </div>
  );
}

function formatEscopoFiliaisTipo(value: UsuarioAdmin['escopoFiliaisTipo']) {
  switch (value) {
    case 'HERDAR_SETOR':
      return 'Herdar do setor';
    case 'TODAS':
      return 'Todas as filiais';
    case 'SELECIONADAS':
      return 'Somente selecionadas';
    default:
      return value;
  }
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border px-3 py-2" style={SOFT_PANEL_STYLE}>
      <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
        {label}
      </div>
      <div className="mt-1 break-words text-sm font-medium" style={{ color: 'var(--color-text)' }}>
        {value || 'Nenhuma'}
      </div>
    </div>
  );
}

function DetailTextBlock({ label, value, fallback }: { label: string; value: string; fallback: string }) {
  return (
    <div className="space-y-1">
      <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
        {label}
      </div>
      <div
        className="max-h-24 overflow-y-auto rounded-xl border px-3 py-2 text-xs leading-relaxed"
        style={{
          backgroundColor: 'var(--color-bg)',
          borderColor: 'var(--color-border)',
          color: 'var(--color-text)',
        }}
      >
        {value || fallback}
      </div>
    </div>
  );
}

function renderUsuarioIdentityCell(row: UsuarioRow) {
  return (
    <div className="min-w-[14rem] whitespace-normal">
      <div className="flex items-center gap-2">
        <p className="text-sm font-semibold leading-tight" style={{ color: 'var(--color-text)' }}>
          {row.nome}
        </p>
        {renderStatusBadge(row.ativo)}
      </div>
      <p className="mt-1 break-all text-xs leading-relaxed" style={{ color: 'var(--color-text-muted)' }}>
        {row.email}
      </p>
    </div>
  );
}

function renderPapelCell(row: UsuarioRow) {
  return (
    <div className="max-w-[15rem] whitespace-normal text-sm leading-relaxed" style={{ color: 'var(--color-text)' }}>
      {row.papelResumo || 'Sem papel'}
    </div>
  );
}

function renderUltimaTelaCell(row: UsuarioRow) {
  const atividade = formatUltimaAtividade(row.ultimaAtividade);

  return (
    <div className="min-w-[12rem] max-w-[15rem] space-y-1 whitespace-normal">
      <div className="flex flex-wrap items-center gap-2">
        {renderOnlineBadge(row.isOnline)}
        <span className="truncate text-sm font-medium" style={{ color: 'var(--color-text)' }} title={row.ultimaRotaAcessada ?? undefined}>
          {row.ultimaRotaAcessada ?? 'Sem registro'}
        </span>
      </div>
      <p className="truncate text-xs" style={{ color: 'var(--color-text-muted)' }} title={atividade}>
        {atividade}
      </p>
    </div>
  );
}

function renderUsuarioDetailsPopover(row: UsuarioRow) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-semibold transition-all duration-150 hover:-translate-y-px hover:bg-[var(--color-bg)] ${FOCUS_RING_CLASS}`}
          style={SECONDARY_BUTTON_STYLE}
          aria-label={`Ver detalhes de ${row.nome}`}
        >
          <Eye size={14} />
          Ver mais
        </button>
      </PopoverTrigger>
      <PopoverContent
        align="end"
        sideOffset={8}
        className="max-h-[min(34rem,calc(100vh-5rem))] overflow-y-auto p-4"
        style={{ width: 'min(30rem, calc(100vw - 24px))' }}
      >
        <div className="space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 className="truncate text-sm font-bold" style={{ color: 'var(--color-text)' }}>
                {row.nome}
              </h3>
              <p className="break-all text-xs" style={{ color: 'var(--color-text-muted)' }}>
                {row.email}
              </p>
            </div>
            {renderStatusBadge(row.ativo)}
          </div>

          <div className="grid gap-2 sm:grid-cols-2">
            <DetailItem label="Setor" value={row.setorNome} />
            <DetailItem label="Papel" value={row.papelResumo || 'Sem papel'} />
            <DetailItem label="Escopo" value={formatEscopoFiliaisTipo(row.escopoFiliaisTipo)} />
            <DetailItem label="Senha" value={row.senhaResumo} />
            <DetailItem label="Última tela" value={row.ultimaRotaAcessada ?? 'Sem registro'} />
            <DetailItem label="Última atividade" value={formatUltimaAtividade(row.ultimaAtividade)} />
          </div>

          <DetailTextBlock label="Filiais efetivas" value={row.filiaisResumo} fallback="Acesso total" />
          <DetailTextBlock
            label="Filiais próprias"
            value={formatFiliaisResumo(row.filiaisPermitidasUsuario, row.escopoFiliaisTipo === 'TODAS')}
            fallback="Nenhuma"
          />
          <DetailTextBlock label="Permissões efetivas" value={row.permissoesResumo} fallback="Sem permissões" />
          <DetailTextBlock label="Concessões individuais" value={row.concessoesResumo} fallback="Nenhuma" />
          <DetailTextBlock label="Negações individuais" value={row.negacoesResumo} fallback="Nenhuma" />
        </div>
      </PopoverContent>
    </Popover>
  );
}

export default function AdminUsuariosPage() {
  const { isAdminPlataforma, isDesenvolvedor } = usePermissions();
  const catalogo = useCatalogoPermissoes();
  const papeis = usePapeisAdmin();
  const setores = useSetoresAdmin();
  const filiais = useFiliais();
  const usuarios = useUsuariosAdmin();
  const resumoSessoes = useResumoSessoesUsuariosAdmin();
  const criarUsuario = useCriarUsuario();
  const atualizarUsuario = useAtualizarUsuario();
  const excluirUsuario = useExcluirUsuario();

  const [editing, setEditing] = useState<UsuarioAdmin | null>(null);
  const [form, setForm] = useState<UsuarioPayload>(FORM_INICIAL);
  const [erro, setErro] = useState('');
  const [overrideState, setOverrideState] = useState<PermissionOverrideStateMap>(createEmptyPermissionOverrideState());
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const isMobileUsersTable = useIsMobileUsersTable();
  const podeOperarPapelElevado = isAdminPlataforma || isDesenvolvedor;
  const filiaisDisponiveis = filiais.data ?? [];
  const senhaEmEdicao = form.senha ?? '';
  const senhaPolicyErrors = useMemo(
    () => (senhaEmEdicao.trim() ? getPasswordPolicyErrors(senhaEmEdicao) : []),
    [senhaEmEdicao],
  );

  const setorSelecionado = useMemo(
    () => (setores.data ?? []).find((setor) => setor.id === form.setorId) ?? null,
    [form.setorId, setores.data],
  );

  const baseline = setorSelecionado?.templatePermissoes ?? createEmptyPermissionMap();
  const papelComAcessoTotal = form.papel === PAPEL_ADMIN_PLATAFORMA
    || (Boolean(PAPEL_DESENVOLVEDOR) && form.papel === PAPEL_DESENVOLVEDOR);
  const escopoComAcessoTotal = papelComAcessoTotal || form.escopoFiliaisTipo === 'TODAS';
  const filiaisEfetivasPreview = useMemo(() => {
    if (escopoComAcessoTotal) {
      return [];
    }

    if (form.escopoFiliaisTipo === 'SELECIONADAS') {
      return [...form.filiaisPermitidasUsuario].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
    }

    return setorSelecionado?.filiaisPermitidas ?? [];
  }, [escopoComAcessoTotal, form.escopoFiliaisTipo, form.filiaisPermitidasUsuario, setorSelecionado?.filiaisPermitidas]);

  usePageHeader({
    title: 'Gestão de usuários',
    description: 'Herança por setor, escopo de filiais e exceções individuais de acesso.',
  });

  const permissoesEfetivasPreview = useMemo<PermissionMap>(() => {
    if (form.papel === PAPEL_ADMIN_PLATAFORMA) {
      const completo = buildPermissionMapFromCatalog(catalogo.data ?? []);
      return Object.keys(completo).reduce((acc, key) => {
        acc[key as keyof PermissionMap] = true;
        return acc;
      }, { ...completo });
    }

    const proximo = { ...baseline };
    for (const [permissaoChave, modo] of Object.entries(overrideState)) {
      if (modo === 'deny') {
        proximo[permissaoChave as keyof PermissionMap] = false;
      } else if (modo === 'grant') {
        proximo[permissaoChave as keyof PermissionMap] = true;
      }
    }
    return proximo;
  }, [baseline, catalogo.data, form.papel, overrideState]);

  const negacoesPreview = useMemo(
    () => mapStateToNegacoes(overrideState)
      .map((chave) => catalogo.data?.find((item) => item.chave === chave)?.nome ?? chave)
      .join(', ') || 'Nenhuma',
    [catalogo.data, overrideState],
  );

  const concessoesPreview = useMemo(
    () => mapStateToConcedidas(overrideState)
      .map((chave) => catalogo.data?.find((item) => item.chave === chave)?.nome ?? chave)
      .join(', ') || 'Nenhuma',
    [catalogo.data, overrideState],
  );

  const linhas = useMemo<UsuarioRow[]>(
    () =>
      (usuarios.data ?? []).map((usuario) => ({
        ...usuario,
        detalhes: usuario.id,
        papelResumo: papeis.data?.find((papel) => papel.nome === usuario.papel)?.descricao ?? formatRoleName(usuario.papel),
        permissoesResumo: permissionSummary(usuario.permissoesEfetivas, catalogo.data ?? []),
        negacoesResumo: usuario.permissoesNegadas
          .map((chave) => catalogo.data?.find((item) => item.chave === chave)?.nome ?? chave)
          .join(', '),
        concessoesResumo: usuario.permissoesConcedidas
          .map((chave) => catalogo.data?.find((item) => item.chave === chave)?.nome ?? chave)
          .join(', '),
        filiaisResumo: formatFiliaisResumo(usuario.filiaisPermitidasEfetivas, usuario.filiaisPermitidasEfetivas.length === 0),
        senhaResumo: `${formatPasswordStatus(usuario.statusSenha)} • ${usuario.algoritmoSenha}`,
        acoes: usuario.id,
      })),
    [catalogo.data, papeis.data, usuarios.data],
  );

  const resumoUsuarios = resumoSessoes.data ?? {
    totalUsuarios: 0,
    usuariosAtivos: 0,
    usuariosInativos: 0,
    usuariosOnline: 0,
  };
  const usuariosOnlineAgora = useMemo(
    () =>
      linhas
        .filter((usuario) => usuario.isOnline)
        .sort((a, b) => timestampAtividade(b.ultimaAtividade) - timestampAtividade(a.ultimaAtividade)),
    [linhas],
  );

  function resetForm() {
    setEditing(null);
    setForm(FORM_INICIAL);
    setErro('');
    setOverrideState(createEmptyPermissionOverrideState());
  }

  function startEdit(usuario: UsuarioAdmin) {
    setEditing(usuario);
    setForm({
      nome: usuario.nome,
      email: usuario.email,
      senha: '',
      confirmacaoSenha: '',
      setorId: usuario.setorId,
      papel: usuario.papel,
      permissoesNegadas: [...usuario.permissoesNegadas],
      permissoesConcedidas: [...usuario.permissoesConcedidas],
      escopoFiliaisTipo: usuario.escopoFiliaisTipo,
      filiaisPermitidasUsuario: [...usuario.filiaisPermitidasUsuario],
      ativo: usuario.ativo,
    });
    setOverrideState(mapOverridesToState(usuario.permissoesNegadas, usuario.permissoesConcedidas));
    setErro('');
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErro('');

    if (form.senha?.trim()) {
      const errors = getPasswordPolicyErrors(form.senha);
      if (errors.length > 0) {
        setErro(errors[0]);
        return;
      }
    }

    if (form.escopoFiliaisTipo === 'SELECIONADAS' && form.filiaisPermitidasUsuario.length === 0) {
      setErro('Selecione ao menos uma filial para o usuário.');
      return;
    }

    const payload: UsuarioPayload = {
      ...form,
      senha: form.senha?.trim() ? form.senha : undefined,
      confirmacaoSenha: form.confirmacaoSenha?.trim() ? form.confirmacaoSenha : undefined,
      permissoesNegadas: mapStateToNegacoes(overrideState),
      permissoesConcedidas: mapStateToConcedidas(overrideState),
      filiaisPermitidasUsuario: form.escopoFiliaisTipo === 'SELECIONADAS' ? form.filiaisPermitidasUsuario : [],
    };

    try {
      if (editing) {
        await atualizarUsuario.mutateAsync({ id: editing.id, payload });
      } else {
        await criarUsuario.mutateAsync(payload);
      }

      resetForm();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  async function handleInativar(usuario: UsuarioAdmin) {
    if (!window.confirm(`Inativar o usuário "${usuario.email}"?`)) return;

    try {
      await excluirUsuario.mutateAsync(usuario.id);
      if (editing?.id === usuario.id) resetForm();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  async function handleTogglePermissaoRapida(usuario: UsuarioAdmin, permissoes: PermissionKey | PermissionKey[], permitir: boolean) {
    const negadas = new Set(usuario.permissoesNegadas);
    const concedidas = new Set(usuario.permissoesConcedidas);
    const permissoesAlvo = Array.isArray(permissoes) ? permissoes : [permissoes];

    for (const permissao of permissoesAlvo) {
      if (permitir) {
        negadas.delete(permissao);
        concedidas.add(permissao);
      } else {
        concedidas.delete(permissao);
        negadas.add(permissao);
      }
    }

    const payload = buildUsuarioPayload(
      usuario,
      Array.from(negadas).sort(),
      Array.from(concedidas).sort(),
    );

    try {
      const atualizado = await atualizarUsuario.mutateAsync({ id: usuario.id, payload });
      if (editing?.id === usuario.id) {
        startEdit(atualizado);
      }
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  const salvando =
    criarUsuario.isPending
    || atualizarUsuario.isPending
    || papeis.isLoading
    || setores.isLoading
    || (form.escopoFiliaisTipo === 'SELECIONADAS' && filiais.isLoading);

  function renderActionMenu(row: UsuarioRow) {
    const usuarioSupremo = row.papel === PAPEL_DESENVOLVEDOR;
    const bloqueado = usuarioSupremo || (!podeOperarPapelElevado && row.papel !== 'usuario_comum');
    const podeGerenciarMetas = Boolean(row.permissoesEfetivas.can_manage_kpi_goals);
    const podeGerenciarComunicacoes = Boolean(row.permissoesEfetivas.can_manage_communications || row.permissoesEfetivas.homeComunicados);
    const operacaoRapidaBloqueada = bloqueado || atualizarUsuario.isPending;

    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            className={`inline-flex h-9 w-9 items-center justify-center rounded-lg border transition-all duration-150 hover:-translate-y-px hover:bg-[var(--color-bg)] ${FOCUS_RING_CLASS}`}
            style={SECONDARY_BUTTON_STYLE}
            aria-label={`Abrir ações de ${row.nome}`}
          >
            <MoreHorizontal size={17} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-[18rem]">
          <DropdownMenuItem
            disabled={bloqueado}
            onSelect={() => startEdit(row)}
            className="gap-2"
          >
            <Pencil size={14} />
            Editar
          </DropdownMenuItem>
          <DropdownMenuItem
            disabled={bloqueado}
            onSelect={() => void handleInativar(row)}
            className="gap-2"
            style={EDIT_DANGER_STYLE}
          >
            <UserX size={14} />
            Inativar
          </DropdownMenuItem>
          <DropdownMenuSeparator
            className="mx-2 my-1 h-px"
            style={{ backgroundColor: 'var(--color-border)' }}
          />
          <DropdownMenuItem
            disabled={operacaoRapidaBloqueada}
            onSelect={() => void handleTogglePermissaoRapida(row, 'can_manage_kpi_goals', !podeGerenciarMetas)}
            className="gap-2"
          >
            <input
              type="checkbox"
              checked={podeGerenciarMetas}
              readOnly
              className="mt-0.5 h-4 w-4 rounded border-[var(--color-border)]"
              style={{ accentColor: 'var(--color-primary)' }}
              tabIndex={-1}
            />
            <span>Permitir gerenciar metas de indicadores</span>
          </DropdownMenuItem>
          <DropdownMenuItem
            disabled={operacaoRapidaBloqueada}
            onSelect={() => void handleTogglePermissaoRapida(row, ['can_manage_communications', 'homeComunicados'], !podeGerenciarComunicacoes)}
            className="gap-2"
          >
            <input
              type="checkbox"
              checked={podeGerenciarComunicacoes}
              readOnly
              className="mt-0.5 h-4 w-4 rounded border-[var(--color-border)]"
              style={{ accentColor: 'var(--color-primary)' }}
              tabIndex={-1}
            />
            <span>Permitir alterar comunicações</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  const colunasUsuariosDesktop: ColunaTabela<UsuarioRow>[] = [
    {
      chave: 'nome',
      label: 'Usuário',
      fixo: true,
      largura: '260px',
      formato: (_, row) => renderUsuarioIdentityCell(row),
    },
    { chave: 'setorNome', label: 'Setor', largura: '180px' },
    {
      chave: 'papelResumo',
      label: 'Papel',
      largura: '240px',
      formato: (_, row) => renderPapelCell(row),
    },
    {
      chave: 'ultimaRotaAcessada',
      label: 'Última tela',
      largura: '230px',
      formato: (_, row) => renderUltimaTelaCell(row),
    },
    {
      chave: 'senhaResumo',
      label: 'Senha',
      largura: '180px',
      ordenavel: false,
      formato: (_, row) => renderPasswordStatusBadge(row.statusSenha, row.algoritmoSenha),
    },
    {
      chave: 'detalhes',
      label: 'Detalhes',
      largura: '120px',
      ordenavel: false,
      formato: (_, row) => renderUsuarioDetailsPopover(row),
    },
    {
      chave: 'acoes',
      label: 'Ações',
      largura: '80px',
      ordenavel: false,
      formato: (_, row) => renderActionMenu(row),
    },
  ];

  const colunasUsuariosMobile: ColunaTabela<UsuarioRow>[] = [
    {
      chave: 'nome',
      label: 'Usuário',
      largura: '220px',
      formato: (_, row) => renderMobileUsuarioCell(row),
    },
    {
      chave: 'setorNome',
      label: 'Acesso',
      largura: '320px',
      ordenavel: false,
      formato: (_, row) => renderMobileAccessCell(row),
    },
    {
      chave: 'acoes',
      label: 'Ações',
      largura: '180px',
      ordenavel: false,
      formato: (_, row) => (
        <div className="flex items-center gap-2">
          {renderOnlineBadge(row.isOnline)}
          {renderUsuarioDetailsPopover(row)}
          {renderActionMenu(row)}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <section className="rounded-[20px] border p-4 shadow-sm sm:p-5" style={SURFACE_STYLE}>
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div className="grid flex-1 grid-cols-1 gap-4 md:grid-cols-4">
            <SummaryCard label="Total de Usuários" value={resumoUsuarios.totalUsuarios} accent="var(--color-text)" />
            <SummaryCard label="Usuários Ativos" value={resumoUsuarios.usuariosAtivos} accent="#10b981" />
            <SummaryCard label="Usuários Inativos" value={resumoUsuarios.usuariosInativos} accent="#ef4444" />
            <SummaryCard
              label="Usuários Online Agora"
              value={resumoUsuarios.usuariosOnline}
              accent="#10b981"
              pulse
              tooltip={<UsuariosOnlineTooltip usuarios={usuariosOnlineAgora} isLoading={usuarios.isLoading} />}
            />
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid w-full gap-4 xl:grid-cols-4">
            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>Nome</span>
              <input
                value={form.nome}
                onChange={(e) => setForm((atual) => ({ ...atual, nome: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>E-mail</span>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm((atual) => ({ ...atual, email: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
                Senha {editing ? '(opcional)' : '(obrigatória)'}
              </span>
              <input
                type="password"
                value={form.senha ?? ''}
                onChange={(e) => setForm((atual) => ({ ...atual, senha: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                minLength={12}
                required={!editing}
              />
              <span className="block text-[11px]" style={{ color: 'var(--color-text-subtle)' }}>
                {PASSWORD_POLICY_HINT}
              </span>
            </label>

            <div className="flex w-full flex-col space-y-1">
              <span className="block text-sm font-medium opacity-0" aria-hidden="true">Importar usuários</span>
              <button
                type="button"
                onClick={() => setIsImportModalOpen(true)}
                className={`inline-flex w-full items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold text-white shadow-sm transition-all duration-150 hover:-translate-y-px hover:opacity-90 ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-primary)' }}
              >
                <Upload size={16} />
                Importar usuários (Excel)
              </button>
            </div>
          </div>

          <div className="grid gap-4 xl:grid-cols-3">
            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
                Confirmar senha {editing ? '(opcional)' : '(obrigatória)'}
              </span>
              <input
                type="password"
                value={form.confirmacaoSenha ?? ''}
                onChange={(e) => setForm((atual) => ({ ...atual, confirmacaoSenha: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                minLength={12}
                required={!editing}
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>Setor</span>
              <select
                value={form.setorId}
                onChange={(e) => setForm((atual) => ({ ...atual, setorId: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                required
              >
                <option value="">Selecione</option>
                {(setores.data ?? []).map((setor) => (
                  <option key={setor.id} value={setor.id}>
                    {setor.nome}
                  </option>
                ))}
              </select>
            </label>

            <div className="flex flex-col justify-end gap-3 rounded-2xl border px-4 py-3" style={SOFT_PANEL_STYLE}>
              <label className="flex items-center gap-2 text-sm" style={{ color: 'var(--color-text)' }}>
                <input
                  type="checkbox"
                  checked={form.ativo}
                  onChange={(e) => setForm((atual) => ({ ...atual, ativo: e.target.checked }))}
                  className="h-4 w-4 rounded border-[var(--color-border)]"
                  style={{ accentColor: 'var(--color-primary)' }}
                />
                Usuário ativo
              </label>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                O papel é único e o e-mail será usado como login da conta.
              </p>
            </div>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Escopo de filiais do usuário</h2>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                Defina se este usuário herda o setor ou usa uma regra própria de filiais.
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-3">
              {ESCOPO_FILIAIS_OPTIONS.map((opcao) => (
                <label
                  key={opcao.value}
                  className="flex items-start gap-3 rounded-2xl border px-4 py-3"
                  style={
                    form.escopoFiliaisTipo === opcao.value
                      ? {
                          backgroundColor: 'var(--color-bg)',
                          borderColor: 'var(--color-primary)',
                        }
                      : SOFT_PANEL_STYLE
                  }
                >
                  <input
                    type="radio"
                    name="escopoFiliaisTipo"
                    checked={form.escopoFiliaisTipo === opcao.value}
                    onChange={() => setForm((atual) => ({
                      ...atual,
                      escopoFiliaisTipo: opcao.value,
                      filiaisPermitidasUsuario: opcao.value === 'SELECIONADAS' ? atual.filiaisPermitidasUsuario : [],
                    }))}
                    className="mt-1 h-4 w-4 border-[var(--color-border)]"
                    style={{ accentColor: 'var(--color-primary)' }}
                  />
                  <div>
                    <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{opcao.label}</div>
                    <div className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>{opcao.description}</div>
                  </div>
                </label>
              ))}
            </div>

            {form.escopoFiliaisTipo === 'SELECIONADAS' && (
              <FiliaisPermitidasSplitSelect
                opcoes={filiaisDisponiveis}
                selecionadas={form.filiaisPermitidasUsuario}
                onChange={(filiaisPermitidasUsuario) => setForm((atual) => ({ ...atual, filiaisPermitidasUsuario }))}
                isLoading={filiais.isLoading}
              />
            )}
          </div>

          {senhaPolicyErrors.length > 0 && (
            <div
              className="rounded-2xl border px-4 py-3 text-sm"
              style={WARNING_PANEL_STYLE}
            >
              {senhaPolicyErrors[0]}
            </div>
          )}

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Papel administrativo</h2>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                Escolha exatamente um papel para definir o alcance administrativo do usuário.
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {(papeis.data ?? []).map((papel: PapelAdmin) => (
                <label
                  key={papel.id}
                  className="flex items-start gap-3 rounded-2xl border px-4 py-3"
                  style={
                    form.papel === papel.nome
                      ? {
                          backgroundColor: 'var(--color-bg)',
                          borderColor: 'var(--color-primary)',
                        }
                      : SOFT_PANEL_STYLE
                  }
                >
                  <input
                    type="radio"
                    name="papel"
                    checked={form.papel === papel.nome}
                    onChange={() => setForm((atual) => ({ ...atual, papel: papel.nome }))}
                    className="mt-1 h-4 w-4 border-[var(--color-border)]"
                    style={{ accentColor: 'var(--color-primary)' }}
                  />
                  <div>
                    <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{formatRoleName(papel.nome)}</div>
                    <div className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>{papel.descricao ?? 'Sem descrição'}</div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Permissões individuais</h2>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                Ajuste exceções de dashboards sobre o baseline herdado do setor.
              </p>
            </div>

            <div className="rounded-2xl border p-3" style={SOFT_PANEL_STYLE}>
              <div className="mb-2 flex items-center justify-between gap-3">
                <h3 className="text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text)' }}>
                  Resumo do acesso
                </h3>
                <span className="text-[11px]" style={{ color: 'var(--color-text-subtle)' }}>
                  Prévia do acesso final
                </span>
              </div>

              <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Setor</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{setorSelecionado?.nome ?? 'Selecione um setor'}</div>
                </div>
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Papel</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{formatRoleName(form.papel)}</div>
                </div>
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Filiais efetivas</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{formatFiliaisResumo(filiaisEfetivasPreview, escopoComAcessoTotal)}</div>
                </div>
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Baseline herdado</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{permissionSummary(baseline, catalogo.data ?? []) || 'Sem permissões'}</div>
                </div>
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Negações</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{negacoesPreview}</div>
                </div>
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Concessões individuais</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{concessoesPreview}</div>
                </div>
                <div className="rounded-xl border px-3 py-2" style={SURFACE_STYLE}>
                  <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Permissões efetivas</div>
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{permissionSummary(permissoesEfetivasPreview, catalogo.data ?? []) || 'Sem permissões'}</div>
                </div>
              </div>
            </div>

            <PermissionOverrideMatrix
              catalogo={catalogo.data ?? []}
              baseline={baseline}
              papel={form.papel}
              valor={overrideState}
              onChange={setOverrideState}
              disabled={catalogo.isLoading || !form.setorId || form.papel === PAPEL_ADMIN_PLATAFORMA}
            />
          </div>

          {erro && <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-200">{erro}</p>}

          <div className="flex flex-wrap gap-3">
            <button
              type="submit"
              disabled={salvando}
              className="rounded-xl bg-[#21478A] px-4 py-2.5 text-sm font-medium text-white disabled:opacity-50"
            >
              {editing ? 'Salvar alterações' : 'Criar usuário'}
            </button>
            {editing && (
              <button
                type="button"
                onClick={resetForm}
                className="rounded-xl border px-4 py-2.5 text-sm font-medium transition-opacity hover:opacity-80"
                style={SECONDARY_BUTTON_STYLE}
              >
                Cancelar edição
              </button>
            )}
          </div>
        </form>
      </section>

      <DataTable
        titulo="Usuários cadastrados"
        dados={linhas}
        chaveLinha="id"
        isLoading={usuarios.isLoading}
        colunas={isMobileUsersTable ? colunasUsuariosMobile : colunasUsuariosDesktop}
      />

      <UsuariosImportacaoModal open={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
    </div>
  );
}
