import { useMemo, useState } from 'react';
import PermissionMatrix from '../components/admin/PermissionMatrix';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable from '../components/shared/DataTable';
import {
  useAtualizarSetor,
  useCatalogoPermissoes,
  useCriarSetor,
  useExcluirSetor,
  useSetoresAdmin,
} from '../hooks/queries/useAdminAcesso';
import { useFiliais } from '../hooks/queries/useDimensoes';
import type { PermissionMap, SetorAdmin, SetorPayload } from '../types/access';
import { createEmptyPermissionMap, permissionSummary } from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';

interface SetorRow extends SetorAdmin {
  permissoesResumo: string;
  filiaisResumo: string;
  acoes: string;
}

const FORM_INICIAL: SetorPayload = {
  nome: '',
  descricao: '',
  permissoes: createEmptyPermissionMap(),
  filiaisPermitidas: [],
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

const SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const ACTIVE_BADGE_STYLE = {
  backgroundColor: 'color-mix(in srgb, var(--color-text) 14%, var(--color-card))',
  color: 'var(--color-text)',
};

const INACTIVE_BADGE_STYLE = {
  backgroundColor: 'var(--color-bg)',
  color: 'var(--color-text-subtle)',
};

const DANGER_BUTTON_STYLE = {
  borderColor: 'color-mix(in srgb, #ef4444 30%, var(--color-border))',
  color: 'color-mix(in srgb, #ef4444 78%, var(--color-text))',
};

export default function AdminSetoresPage() {
  const catalogo = useCatalogoPermissoes();
  const setores = useSetoresAdmin();
  const filiais = useFiliais();
  const criarSetor = useCriarSetor();
  const atualizarSetor = useAtualizarSetor();
  const excluirSetor = useExcluirSetor();

  const [editing, setEditing] = useState<SetorAdmin | null>(null);
  const [form, setForm] = useState<SetorPayload>(FORM_INICIAL);
  const [erro, setErro] = useState('');
  const filiaisDisponiveis = filiais.data ?? [];
  const todasFiliaisSelecionadas = filiaisDisponiveis.length > 0
    && filiaisDisponiveis.every((filial) => form.filiaisPermitidas.includes(filial));

  const linhas = useMemo<SetorRow[]>(
    () =>
      (setores.data ?? []).map((setor) => ({
        ...setor,
        permissoesResumo: permissionSummary(setor.templatePermissoes, catalogo.data ?? []),
        filiaisResumo: setor.filiaisPermitidas.join(', '),
        acoes: setor.id,
      })),
    [catalogo.data, setores.data],
  );

  function resetForm() {
    setEditing(null);
    setForm(FORM_INICIAL);
    setErro('');
  }

  function startEdit(setor: SetorAdmin) {
    setEditing(setor);
    setForm({
      nome: setor.nome,
      descricao: setor.descricao,
      permissoes: { ...setor.templatePermissoes } as PermissionMap,
      filiaisPermitidas: [...setor.filiaisPermitidas],
    });
    setErro('');
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setErro('');

    try {
      if (editing) {
        await atualizarSetor.mutateAsync({ id: editing.id, payload: form });
      } else {
        await criarSetor.mutateAsync(form);
      }
      resetForm();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  async function handleDelete(setor: SetorAdmin) {
    if (!window.confirm(`Excluir o setor "${setor.nome}"?`)) return;

    try {
      await excluirSetor.mutateAsync(setor.id);
      if (editing?.id === setor.id) resetForm();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  function selecionarTodasFiliais() {
    if (filiaisDisponiveis.length === 0) return;
    setForm((atual) => ({ ...atual, filiaisPermitidas: [...filiaisDisponiveis] }));
  }

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border p-6 shadow-sm" style={SURFACE_STYLE}>
        <div className="mb-6">
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Gestão de setores</h1>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            O setor define o baseline de dashboards e o escopo de filiais que cada usuário herdará.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>Nome do setor</span>
              <input
                value={form.nome}
                onChange={(e) => setForm((atual) => ({ ...atual, nome: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                placeholder="Ex: Financeiro"
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>Descrição</span>
              <input
                value={form.descricao ?? ''}
                onChange={(e) => setForm((atual) => ({ ...atual, descricao: e.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                placeholder="Resumo do setor"
              />
            </label>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Escopo de filiais</h2>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>Usuários deste setor só verão dados das filiais selecionadas.</p>
            </div>
            <div className="flex flex-wrap items-start gap-3">
              <AsyncMultiSelect
                label="Filiais permitidas"
                opcoes={filiaisDisponiveis}
                selecionados={form.filiaisPermitidas}
                onChange={(filiaisPermitidas) => setForm((atual) => ({ ...atual, filiaisPermitidas }))}
                placeholder="Selecione ao menos uma filial"
                isLoading={filiais.isLoading}
              />
              <button
                type="button"
                onClick={selecionarTodasFiliais}
                disabled={filiais.isLoading || filiaisDisponiveis.length === 0 || todasFiliaisSelecionadas}
                className="rounded-xl border px-4 py-2.5 text-sm font-medium transition-opacity hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                style={SECONDARY_BUTTON_STYLE}
              >
                {todasFiliaisSelecionadas ? 'Todas selecionadas' : 'Selecionar todas'}
              </button>
            </div>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Template de acesso do setor</h2>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>Cada item habilita o acesso base herdado pelos usuários do setor.</p>
            </div>
            <PermissionMatrix
              catalogo={catalogo.data ?? []}
              valor={form.permissoes}
              onChange={(permissoes) => setForm((atual) => ({ ...atual, permissoes }))}
              disabled={catalogo.isLoading}
            />
          </div>

          {erro && <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-200">{erro}</p>}
          {!erro && form.filiaisPermitidas.length === 0 && (
            <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
              Selecione pelo menos uma filial para salvar o setor.
            </p>
          )}

          <div className="flex flex-wrap gap-3">
            <button
              type="submit"
              disabled={criarSetor.isPending || atualizarSetor.isPending || form.filiaisPermitidas.length === 0}
              className="rounded-xl bg-[#21478A] px-4 py-2.5 text-sm font-medium text-white disabled:opacity-50"
            >
              {editing ? 'Salvar alterações' : 'Criar setor'}
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
        titulo="Setores cadastrados"
        dados={linhas}
        chaveLinha="id"
        isLoading={setores.isLoading}
        colunas={[
          { chave: 'nome', label: 'Setor', fixo: true },
          { chave: 'descricao', label: 'Descrição' },
          { chave: 'totalUsuarios', label: 'Usuários' },
          {
            chave: 'filiaisResumo',
            label: 'Filiais',
            formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Nenhuma filial')}</span>,
          },
          {
            chave: 'sistema',
            label: 'Sistema',
            formato: (valor) => (
              <span
                className="rounded-full px-2 py-1 text-xs font-medium"
                style={valor ? ACTIVE_BADGE_STYLE : INACTIVE_BADGE_STYLE}
              >
                {valor ? 'Sim' : 'Não'}
              </span>
            ),
          },
          {
            chave: 'permissoesResumo',
            label: 'Baseline de acesso',
            formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Sem permissões')}</span>,
          },
          {
            chave: 'acoes',
            label: 'Ações',
            formato: (_, row) => (
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => startEdit(row)}
                  className="rounded-lg border px-3 py-1.5 text-xs font-medium transition-opacity hover:opacity-80"
                  style={SECONDARY_BUTTON_STYLE}
                >
                  Editar
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(row)}
                  disabled={row.sistema}
                  className="rounded-lg border px-3 py-1.5 text-xs font-medium disabled:cursor-not-allowed disabled:opacity-40"
                  style={DANGER_BUTTON_STYLE}
                >
                  Excluir
                </button>
              </div>
            ),
          },
        ]}
      />
    </div>
  );
}
