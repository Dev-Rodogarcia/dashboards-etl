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
import { createEmptyPermissionMap } from '../utils/accessControl';
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

  const linhas = useMemo<SetorRow[]>(
    () =>
      (setores.data ?? []).map((setor) => ({
        ...setor,
        permissoesResumo: catalogo.data
          ?.filter((item) => setor.permissoes[item.chave])
          .map((item) => item.nome)
          .join(', ') ?? '',
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
      permissoes: { ...setor.permissoes } as PermissionMap,
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

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Gestão de setores</h1>
          <p className="mt-1 text-sm text-gray-500">
            Defina quais dashboards cada perfil pode acessar.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">Nome do setor</span>
              <input
                value={form.nome}
                onChange={(e) => setForm((atual) => ({ ...atual, nome: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
                placeholder="Ex: Financeiro"
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">Descrição</span>
              <input
                value={form.descricao ?? ''}
                onChange={(e) => setForm((atual) => ({ ...atual, descricao: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
                placeholder="Resumo do perfil"
              />
            </label>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold text-gray-900">Escopo de filiais</h2>
              <p className="text-xs text-gray-500">Usuários deste setor só verão dados das filiais selecionadas.</p>
            </div>
            <AsyncMultiSelect
              label="Filiais permitidas"
              opcoes={filiais.data ?? []}
              selecionados={form.filiaisPermitidas}
              onChange={(filiaisPermitidas) => setForm((atual) => ({ ...atual, filiaisPermitidas }))}
              placeholder="Selecione ao menos uma filial"
              isLoading={filiais.isLoading}
            />
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold text-gray-900">Permissões do setor</h2>
              <p className="text-xs text-gray-500">Cada item habilita um dashboard ou recurso de apoio.</p>
            </div>
            <PermissionMatrix
              catalogo={catalogo.data ?? []}
              valor={form.permissoes}
              onChange={(permissoes) => setForm((atual) => ({ ...atual, permissoes }))}
              disabled={catalogo.isLoading}
            />
          </div>

          {erro && <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}
          {!erro && form.filiaisPermitidas.length === 0 && (
            <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
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
                className="rounded-xl border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700"
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
              <span className={`rounded-full px-2 py-1 text-xs font-medium ${valor ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600'}`}>
                {valor ? 'Sim' : 'Não'}
              </span>
            ),
          },
          {
            chave: 'permissoesResumo',
            label: 'Permissões',
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
                  className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700"
                >
                  Editar
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(row)}
                  disabled={row.sistema}
                  className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 disabled:cursor-not-allowed disabled:opacity-40"
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
