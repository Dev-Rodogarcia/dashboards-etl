import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import PermissionOverrideMatrix from '../components/admin/PermissionOverrideMatrix';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import {
  useAtualizarUsuario,
  useCatalogoPermissoes,
  useCriarUsuario,
  useExcluirUsuario,
  usePapeisAdmin,
  useSetoresAdmin,
  useUsuariosAdmin,
} from '../hooks/queries/useAdminAcesso';
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
  permissionSummary,
} from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';

interface UsuarioRow extends UsuarioAdmin {
  acoes: string;
  papelResumo: string;
  permissoesResumo: string;
  negacoesResumo: string;
  filiaisResumo: string;
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
  ativo: true,
};

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

const ACTIVE_BADGE_STYLE = {
  backgroundColor: 'color-mix(in srgb, #10b981 14%, var(--color-card))',
  color: 'color-mix(in srgb, #10b981 72%, var(--color-text))',
};

const INACTIVE_BADGE_STYLE = {
  backgroundColor: 'color-mix(in srgb, #ef4444 14%, var(--color-card))',
  color: 'color-mix(in srgb, #ef4444 72%, var(--color-text))',
};

function formatRoleName(nome: string): string {
  return nome
    .split('_')
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(' ');
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

function ExpandableMobileText({
  value,
  fallback,
  maxLength = 72,
}: {
  value: string;
  fallback: string;
  maxLength?: number;
}) {
  const [expanded, setExpanded] = useState(false);
  const text = value.trim() || fallback;
  const canExpand = text.length > maxLength;
  const content = canExpand && !expanded ? `${text.slice(0, maxLength).trimEnd()}...` : text;

  return (
    <div className="space-y-1">
      <span style={{ color: 'var(--color-text)' }}>{content}</span>
      {canExpand && (
        <button
          type="button"
          onClick={() => setExpanded((current) => !current)}
          className="inline-flex rounded-md text-[11px] font-semibold transition-opacity hover:opacity-75"
          style={{ color: 'var(--color-primary)' }}
        >
          {expanded ? 'Ver menos' : 'Ver mais'}
        </button>
      )}
    </div>
  );
}

function renderStatusBadge(ativo: boolean) {
  return (
    <span
      className="inline-flex w-fit rounded-full px-2 py-1 text-xs font-medium"
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
    <div className="min-w-[16rem] space-y-2 whitespace-normal text-xs leading-relaxed">
      <div className="flex flex-wrap items-center gap-2">
        {renderStatusBadge(row.ativo)}
        <span className="font-medium" style={{ color: 'var(--color-text)' }}>
          {row.setorNome}
        </span>
      </div>

      <div className="space-y-2 break-words">
        <div>
          <span className="block text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
            Papel
          </span>
          <span style={{ color: 'var(--color-text)' }}>{row.papelResumo || 'Sem papel'}</span>
        </div>
        <div>
          <span className="block text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
            Filiais
          </span>
          <ExpandableMobileText value={row.filiaisResumo} fallback="Acesso total" maxLength={52} />
        </div>
        <div>
          <span className="block text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
            Permissões
          </span>
          <ExpandableMobileText value={row.permissoesResumo} fallback="Sem permissões" maxLength={56} />
        </div>
        <div>
          <span className="block text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
            Negações
          </span>
          <span style={{ color: 'var(--color-text)' }}>{row.negacoesResumo || 'Nenhuma'}</span>
        </div>
      </div>
    </div>
  );
}

export default function AdminUsuariosPage() {
  const { isAdminPlataforma } = usePermissions();
  const catalogo = useCatalogoPermissoes();
  const papeis = usePapeisAdmin();
  const setores = useSetoresAdmin();
  const usuarios = useUsuariosAdmin();
  const criarUsuario = useCriarUsuario();
  const atualizarUsuario = useAtualizarUsuario();
  const excluirUsuario = useExcluirUsuario();

  const [editing, setEditing] = useState<UsuarioAdmin | null>(null);
  const [form, setForm] = useState<UsuarioPayload>(FORM_INICIAL);
  const [erro, setErro] = useState('');
  const [overrideState, setOverrideState] = useState<PermissionOverrideStateMap>(createEmptyPermissionOverrideState());
  const isMobileUsersTable = useIsMobileUsersTable();

  const setorSelecionado = useMemo(
    () => (setores.data ?? []).find((setor) => setor.id === form.setorId) ?? null,
    [form.setorId, setores.data],
  );

  const baseline = setorSelecionado?.templatePermissoes ?? createEmptyPermissionMap();

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
        papelResumo: papeis.data?.find((papel) => papel.nome === usuario.papel)?.descricao ?? formatRoleName(usuario.papel),
        permissoesResumo: permissionSummary(usuario.permissoesEfetivas, catalogo.data ?? []),
        negacoesResumo: usuario.permissoesNegadas
          .map((chave) => catalogo.data?.find((item) => item.chave === chave)?.nome ?? chave)
          .join(', '),
        filiaisResumo: usuario.filiaisPermitidasEfetivas.join(', '),
        acoes: usuario.id,
      })),
    [catalogo.data, papeis.data, usuarios.data],
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
      ativo: usuario.ativo,
    });
    setOverrideState(mapOverridesToState(usuario.permissoesNegadas, usuario.permissoesConcedidas));
    setErro('');
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErro('');

    const payload: UsuarioPayload = {
      ...form,
      senha: form.senha?.trim() ? form.senha : undefined,
      confirmacaoSenha: form.confirmacaoSenha?.trim() ? form.confirmacaoSenha : undefined,
      permissoesNegadas: mapStateToNegacoes(overrideState),
      permissoesConcedidas: mapStateToConcedidas(overrideState),
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

  async function handleDelete(usuario: UsuarioAdmin) {
    if (!window.confirm(`Inativar o usuário "${usuario.email}"?`)) return;

    try {
      await excluirUsuario.mutateAsync(usuario.id);
      if (editing?.id === usuario.id) resetForm();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  const salvando =
    criarUsuario.isPending
    || atualizarUsuario.isPending
    || papeis.isLoading
    || setores.isLoading;

  function renderActionButtons(row: UsuarioRow, compacto = false) {
    const bloqueado = !isAdminPlataforma && row.papel !== 'usuario_comum';

    return (
      <div className={compacto ? 'flex min-w-[8.5rem] flex-col gap-2' : 'flex gap-2'}>
        <button
          type="button"
          onClick={() => startEdit(row)}
          disabled={bloqueado}
          className={`rounded-lg border px-3 py-1.5 text-xs font-medium disabled:cursor-not-allowed disabled:opacity-40 ${compacto ? 'w-full text-center' : ''}`}
          style={SECONDARY_BUTTON_STYLE}
        >
          Editar
        </button>
        <button
          type="button"
          onClick={() => handleDelete(row)}
          disabled={bloqueado}
          className={`rounded-lg border px-3 py-1.5 text-xs font-medium disabled:cursor-not-allowed disabled:opacity-40 ${compacto ? 'w-full text-center' : ''}`}
          style={EDIT_DANGER_STYLE}
        >
          Inativar
        </button>
      </div>
    );
  }

  const colunasUsuariosDesktop: ColunaTabela<UsuarioRow>[] = [
    { chave: 'nome', label: 'Nome', fixo: true },
    { chave: 'email', label: 'E-mail' },
    { chave: 'setorNome', label: 'Setor' },
    {
      chave: 'papelResumo',
      label: 'Papel',
      formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Sem papel')}</span>,
    },
    {
      chave: 'ativo',
      label: 'Ativo',
      formato: (valor) => renderStatusBadge(Boolean(valor)),
    },
    {
      chave: 'filiaisResumo',
      label: 'Filiais',
      formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Acesso total')}</span>,
    },
    {
      chave: 'permissoesResumo',
      label: 'Permissões efetivas',
      formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Sem permissões')}</span>,
    },
    {
      chave: 'negacoesResumo',
      label: 'Negações',
      formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Nenhuma')}</span>,
    },
    {
      chave: 'acoes',
      label: 'Ações',
      ordenavel: false,
      formato: (_, row) => renderActionButtons(row),
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
      largura: '160px',
      ordenavel: false,
      formato: (_, row) => renderActionButtons(row, true),
    },
  ];

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border p-6 shadow-sm" style={SURFACE_STYLE}>
        <div className="mb-6">
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Gestão de usuários</h1>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            O usuário herda o acesso do setor e pode receber apenas negações individuais de dashboard.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
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
                required={!editing}
              />
            </label>

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
              <h2 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Negações individuais</h2>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                O usuário herda o setor. Aqui você só nega dashboards específicos.
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
                  <div className="mt-1 text-sm font-medium" style={{ color: 'var(--color-text)' }}>{setorSelecionado?.filiaisPermitidas.join(', ') || 'Nenhuma'}</div>
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
    </div>
  );
}
