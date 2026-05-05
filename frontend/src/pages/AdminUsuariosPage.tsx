import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Eye, MoreHorizontal, Pencil, Trash2, Upload, UserX } from 'lucide-react';
import PermissionOverrideMatrix from '../components/admin/PermissionOverrideMatrix';
import UsuariosImportacaoModal from '../components/admin/UsuariosImportacaoModal';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import { usePageHeader } from '../contexts/PageHeaderContext';
import {
  useAtualizarUsuario,
  useCatalogoPermissoes,
  useCriarUsuario,
  useExcluirUsuarioDefinitivamente,
  useExcluirUsuario,
  usePapeisAdmin,
  useSetoresAdmin,
  useUsuariosAdmin,
} from '../hooks/queries/useAdminAcesso';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { usePermissions } from '../hooks/usePermissions';
import type {
  PapelAdmin,
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
  backgroundColor: 'color-mix(in srgb, var(--color-text) 6%, var(--color-card))',
  borderColor: 'var(--color-border)',
};

const SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const EDIT_DANGER_STYLE = {
  borderColor: 'color-mix(in srgb, #ef4444 30%, var(--color-border))',
  color: 'color-mix(in srgb, #ef4444 78%, var(--color-text))',
};

const FOCUS_RING_CLASS = 'outline-none focus-visible:ring-2 focus-visible:ring-[color-mix(in_srgb,var(--color-primary)_34%,transparent)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-card)]';

const ACTIVE_BADGE_STYLE = {
  backgroundColor: 'color-mix(in srgb, #10b981 14%, var(--color-card))',
  color: 'color-mix(in srgb, #10b981 72%, var(--color-text))',
};

const INACTIVE_BADGE_STYLE = {
  backgroundColor: 'color-mix(in srgb, #ef4444 14%, var(--color-card))',
  color: 'color-mix(in srgb, #ef4444 72%, var(--color-text))',
};

const PASSWORD_STATUS_STYLE = {
  segura: {
    backgroundColor: 'color-mix(in srgb, #10b981 14%, var(--color-card))',
    color: 'color-mix(in srgb, #10b981 74%, var(--color-text))',
  },
  migrar_no_login: {
    backgroundColor: 'color-mix(in srgb, #f59e0b 14%, var(--color-card))',
    color: 'color-mix(in srgb, #b45309 72%, var(--color-text))',
  },
  reset_obrigatorio: {
    backgroundColor: 'color-mix(in srgb, #ef4444 14%, var(--color-card))',
    color: 'color-mix(in srgb, #b91c1c 76%, var(--color-text))',
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

function SummaryCard({ label, value, accent }: { label: string; value: number; accent: string }) {
  return (
    <div className="rounded-xl border px-3 py-2.5" style={SURFACE_STYLE}>
      <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
        {label}
      </div>
      <div className="mt-1 text-xl font-bold" style={{ color: accent }}>
        {value}
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
  const { canHardDeleteUsers, isAdminPlataforma, isDesenvolvedor } = usePermissions();
  const catalogo = useCatalogoPermissoes();
  const papeis = usePapeisAdmin();
  const setores = useSetoresAdmin();
  const filiais = useFiliais();
  const usuarios = useUsuariosAdmin();
  const criarUsuario = useCriarUsuario();
  const atualizarUsuario = useAtualizarUsuario();
  const excluirUsuario = useExcluirUsuario();
  const excluirUsuarioDefinitivamente = useExcluirUsuarioDefinitivamente();

  const [editing, setEditing] = useState<UsuarioAdmin | null>(null);
  const [form, setForm] = useState<UsuarioPayload>(FORM_INICIAL);
  const [erro, setErro] = useState('');
  const [overrideState, setOverrideState] = useState<PermissionOverrideStateMap>(createEmptyPermissionOverrideState());
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<UsuarioAdmin | null>(null);
  const [confirmacaoExclusao, setConfirmacaoExclusao] = useState('');
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
  const todasFiliaisUsuarioSelecionadas = filiaisDisponiveis.length > 0
    && filiaisDisponiveis.every((filial) => form.filiaisPermitidasUsuario.includes(filial));

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

  const resumoStatusSenha = useMemo(() => {
    return linhas.reduce(
      (acc, usuario) => {
        acc.total += 1;
        acc[usuario.statusSenha] += 1;
        return acc;
      },
      {
        total: 0,
        segura: 0,
        migrar_no_login: 0,
        reset_obrigatorio: 0,
      },
    );
  }, [linhas]);

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

  function selecionarTodasFiliaisUsuario() {
    if (filiaisDisponiveis.length === 0) return;
    setForm((atual) => ({ ...atual, filiaisPermitidasUsuario: [...filiaisDisponiveis] }));
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

  function abrirModalExclusao(usuario: UsuarioAdmin) {
    setErro('');
    setConfirmacaoExclusao('');
    setUsuarioParaExcluir(usuario);
  }

  function fecharModalExclusao() {
    if (excluirUsuarioDefinitivamente.isPending) return;
    setUsuarioParaExcluir(null);
    setConfirmacaoExclusao('');
  }

  async function confirmarExclusaoDefinitiva() {
    if (!usuarioParaExcluir || confirmacaoExclusao !== 'EXCLUIR') return;

    try {
      await excluirUsuarioDefinitivamente.mutateAsync(usuarioParaExcluir.id);
      if (editing?.id === usuarioParaExcluir.id) resetForm();
      fecharModalExclusao();
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
    const podeExcluirDefinitivamente = canHardDeleteUsers && !usuarioSupremo;

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
        <DropdownMenuContent align="end" className="min-w-[12rem]">
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
          {podeExcluirDefinitivamente && (
            <>
              <DropdownMenuSeparator
                className="mx-2 my-1 h-px"
                style={{ backgroundColor: 'var(--color-border)' }}
              />
              <DropdownMenuItem
                disabled={excluirUsuarioDefinitivamente.isPending}
                onSelect={() => abrirModalExclusao(row)}
                className="gap-2 font-semibold"
                style={{ color: 'color-mix(in srgb, #dc2626 86%, var(--color-text))' }}
              >
                <Trash2 size={14} />
                Excluir definitivamente
              </DropdownMenuItem>
            </>
          )}
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
          <div className="grid flex-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <SummaryCard label="Usuários monitorados" value={resumoStatusSenha.total} accent="var(--color-text)" />
            <SummaryCard label="Hash seguro" value={resumoStatusSenha.segura} accent="#10b981" />
            <SummaryCard label="Migra no login" value={resumoStatusSenha.migrar_no_login} accent="#f59e0b" />
            <SummaryCard label="Reset obrigatório" value={resumoStatusSenha.reset_obrigatorio} accent="#ef4444" />
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
                  className="h-4 w-4 rounded border-gray-300"
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
                          backgroundColor: 'color-mix(in srgb, var(--color-primary) 12%, var(--color-card))',
                          borderColor: 'color-mix(in srgb, var(--color-primary) 34%, var(--color-border))',
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
                    className="mt-1 h-4 w-4 border-gray-300"
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
              <div className="flex flex-wrap items-start gap-3">
                <AsyncMultiSelect
                  label="Filiais do usuário"
                  opcoes={filiaisDisponiveis}
                  selecionados={form.filiaisPermitidasUsuario}
                  onChange={(filiaisPermitidasUsuario) => setForm((atual) => ({ ...atual, filiaisPermitidasUsuario }))}
                  placeholder="Selecione ao menos uma filial"
                  isLoading={filiais.isLoading}
                />
                <button
                  type="button"
                  onClick={selecionarTodasFiliaisUsuario}
                  disabled={filiais.isLoading || filiaisDisponiveis.length === 0 || todasFiliaisUsuarioSelecionadas}
                  className="rounded-xl border px-4 py-2.5 text-sm font-medium transition-opacity hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                  style={SECONDARY_BUTTON_STYLE}
                >
                  {todasFiliaisUsuarioSelecionadas ? 'Todas selecionadas' : 'Selecionar todas'}
                </button>
              </div>
            )}
          </div>

          {senhaPolicyErrors.length > 0 && (
            <div
              className="rounded-2xl border px-4 py-3 text-sm"
              style={{
                backgroundColor: 'color-mix(in srgb, #f59e0b 10%, var(--color-card))',
                borderColor: 'color-mix(in srgb, #f59e0b 28%, var(--color-border))',
                color: 'color-mix(in srgb, #b45309 72%, var(--color-text))',
              }}
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
                          backgroundColor: 'color-mix(in srgb, var(--color-primary) 12%, var(--color-card))',
                          borderColor: 'color-mix(in srgb, var(--color-primary) 34%, var(--color-border))',
                        }
                      : SOFT_PANEL_STYLE
                  }
                >
                  <input
                    type="radio"
                    name="papel"
                    checked={form.papel === papel.nome}
                    onChange={() => setForm((atual) => ({ ...atual, papel: papel.nome }))}
                    className="mt-1 h-4 w-4 border-gray-300"
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

      {usuarioParaExcluir && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-lg rounded-2xl border p-5 shadow-xl" style={SURFACE_STYLE}>
            <div className="space-y-2">
              <h2 className="text-lg font-bold" style={{ color: 'var(--color-text)' }}>Excluir usuário</h2>
              <p className="text-sm leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
                Esta ação remove definitivamente a conta de {usuarioParaExcluir.email} e limpa vínculos de acesso, sessões e referências dependentes.
              </p>
            </div>

            <label className="mt-5 block space-y-2">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>
                Digite EXCLUIR para confirmar
              </span>
              <input
                value={confirmacaoExclusao}
                onChange={(event) => setConfirmacaoExclusao(event.target.value)}
                className="w-full rounded-xl border px-3 py-2.5 font-mono"
                style={FIELD_STYLE}
                autoFocus
              />
            </label>

            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button
                type="button"
                onClick={fecharModalExclusao}
                disabled={excluirUsuarioDefinitivamente.isPending}
                className="rounded-xl border px-4 py-2.5 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-50"
                style={SECONDARY_BUTTON_STYLE}
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={confirmarExclusaoDefinitiva}
                disabled={confirmacaoExclusao !== 'EXCLUIR' || excluirUsuarioDefinitivamente.isPending}
                className="rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
                style={{ backgroundColor: '#dc2626' }}
              >
                Excluir definitivamente
              </button>
            </div>
          </div>
        </div>
      )}

      <UsuariosImportacaoModal open={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
    </div>
  );
}
