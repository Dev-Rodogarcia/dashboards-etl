import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import PermissionOverrideMatrix from '../components/admin/PermissionOverrideMatrix';
import DataTable from '../components/shared/DataTable';
import {
  useAtribuirPapeisUsuario,
  useAtualizarUsuario,
  useCatalogoPermissoes,
  useCriarUsuario,
  useExcluirUsuario,
  usePapeisAdmin,
  useSalvarOverridesUsuario,
  useSetoresAdmin,
  useUsuarioOverridesAdmin,
  useUsuariosAdmin,
} from '../hooks/queries/useAdminAcesso';
import type {
  PapelAdmin,
  PermissionOverrideStateMap,
  PermissaoOverride,
  UsuarioAdmin,
  UsuarioPayload,
} from '../types/access';
import { createEmptyPermissionOverrideState } from '../utils/accessControl';
import { getApiErrorMessage } from '../utils/apiError';

interface UsuarioRow extends UsuarioAdmin {
  acoes: string;
  papeisResumo: string;
  permissoesResumo: string;
}

const FORM_INICIAL: UsuarioPayload = {
  login: '',
  nome: '',
  email: '',
  senha: '',
  setorId: '',
  admin: false,
  ativo: true,
};

const PAPEIS_INICIAIS = ['usuario_comum'];

function formatRoleName(nome: string): string {
  return nome
    .split('_')
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(' ');
}

function formatRoleSummary(papeis: string[], catalogo: PapelAdmin[] | undefined): string {
  if (papeis.length === 0) return 'Sem papéis';

  return papeis
    .map((papel) => catalogo?.find((item) => item.nome === papel)?.descricao ?? formatRoleName(papel))
    .join(', ');
}

function mapOverridesToState(overrides: PermissaoOverride[]): PermissionOverrideStateMap {
  const proximo = createEmptyPermissionOverrideState();

  for (const override of overrides) {
    proximo[override.permissaoChave] = override.tipo === 'GRANT' ? 'grant' : 'deny';
  }

  return proximo;
}

function mapStateToOverrides(state: PermissionOverrideStateMap): PermissaoOverride[] {
  return Object.entries(state)
    .filter(([, valor]) => valor !== 'inherit')
    .map(([permissaoChave, valor]) => ({
      permissaoChave: permissaoChave as PermissaoOverride['permissaoChave'],
      tipo: valor === 'grant' ? 'GRANT' : 'DENY',
    }));
}

export default function AdminUsuariosPage() {
  const catalogo = useCatalogoPermissoes();
  const papeis = usePapeisAdmin();
  const setores = useSetoresAdmin();
  const usuarios = useUsuariosAdmin();
  const criarUsuario = useCriarUsuario();
  const atualizarUsuario = useAtualizarUsuario();
  const excluirUsuario = useExcluirUsuario();
  const atribuirPapeisUsuario = useAtribuirPapeisUsuario();
  const salvarOverridesUsuario = useSalvarOverridesUsuario();

  const [editing, setEditing] = useState<UsuarioAdmin | null>(null);
  const [form, setForm] = useState<UsuarioPayload>(FORM_INICIAL);
  const [erro, setErro] = useState('');
  const [papeisSelecionados, setPapeisSelecionados] = useState<string[]>(PAPEIS_INICIAIS);
  const [overrideState, setOverrideState] = useState<PermissionOverrideStateMap>(createEmptyPermissionOverrideState());

  const overridesUsuario = useUsuarioOverridesAdmin(editing?.id);

  useEffect(() => {
    if (!editing) {
      setOverrideState(createEmptyPermissionOverrideState());
      return;
    }

    if (overridesUsuario.data) {
      setOverrideState(mapOverridesToState(overridesUsuario.data));
      return;
    }

    if (!overridesUsuario.isLoading && !overridesUsuario.isFetching) {
      setOverrideState(createEmptyPermissionOverrideState());
    }
  }, [editing, overridesUsuario.data, overridesUsuario.isFetching, overridesUsuario.isLoading]);

  useEffect(() => {
    if (editing && overridesUsuario.error) {
      setErro(getApiErrorMessage(overridesUsuario.error, 'Não foi possível carregar os overrides do usuário.'));
    }
  }, [editing, overridesUsuario.error]);

  const linhas = useMemo<UsuarioRow[]>(
    () =>
      (usuarios.data ?? []).map((usuario) => ({
        ...usuario,
        papeisResumo: formatRoleSummary(usuario.papeis, papeis.data),
        permissoesResumo: catalogo.data
          ?.filter((item) => usuario.permissoes[item.chave])
          .map((item) => item.nome)
          .join(', ') ?? '',
        acoes: usuario.id,
      })),
    [catalogo.data, papeis.data, usuarios.data],
  );

  function resetForm() {
    setEditing(null);
    setForm(FORM_INICIAL);
    setErro('');
    setPapeisSelecionados(PAPEIS_INICIAIS);
    setOverrideState(createEmptyPermissionOverrideState());
  }

  function togglePapel(nomePapel: string) {
    setPapeisSelecionados((atuais) =>
      atuais.includes(nomePapel)
        ? atuais.filter((item) => item !== nomePapel)
        : [...atuais, nomePapel],
    );
  }

  function startEdit(usuario: UsuarioAdmin) {
    setEditing(usuario);
    setForm({
      login: usuario.login,
      nome: usuario.nome,
      email: usuario.email,
      senha: '',
      setorId: usuario.setorId,
      admin: usuario.papeis.includes('admin_plataforma'),
      ativo: usuario.ativo,
    });
    setPapeisSelecionados(usuario.papeis.length > 0 ? usuario.papeis : PAPEIS_INICIAIS);
    setOverrideState(createEmptyPermissionOverrideState());
    setErro('');
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErro('');

    if (papeisSelecionados.length === 0) {
      setErro('Selecione ao menos um papel para o usuário.');
      return;
    }

    const papelIds = (papeis.data ?? [])
      .filter((papel) => papeisSelecionados.includes(papel.nome))
      .map((papel) => papel.id);

    if (papelIds.length !== papeisSelecionados.length) {
      setErro('Não foi possível resolver todos os papéis selecionados.');
      return;
    }

    const payload: UsuarioPayload = {
      ...form,
      admin: papeisSelecionados.includes('admin_plataforma'),
    };

    try {
      const usuarioSalvo = editing
        ? await atualizarUsuario.mutateAsync({ id: editing.id, payload })
        : await criarUsuario.mutateAsync(payload);

      await atribuirPapeisUsuario.mutateAsync({ id: usuarioSalvo.id, papelIds });
      await salvarOverridesUsuario.mutateAsync({
        id: usuarioSalvo.id,
        overrides: mapStateToOverrides(overrideState),
      });

      resetForm();
    } catch (error) {
      setErro(getApiErrorMessage(error));
    }
  }

  async function handleDelete(usuario: UsuarioAdmin) {
    if (!window.confirm(`Excluir o usuário "${usuario.login}"?`)) return;

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
    || atribuirPapeisUsuario.isPending
    || salvarOverridesUsuario.isPending
    || papeis.isLoading
    || (Boolean(editing) && (overridesUsuario.isLoading || overridesUsuario.isFetching));

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Gestão de usuários</h1>
          <p className="mt-1 text-sm text-gray-500">
            Cada usuário pertence a um setor, recebe papéis administrativos e pode ter exceções de permissão.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">Login</span>
              <input
                value={form.login}
                onChange={(e) => setForm((atual) => ({ ...atual, login: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
                placeholder="ex: lucas"
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">Nome</span>
              <input
                value={form.nome}
                onChange={(e) => setForm((atual) => ({ ...atual, nome: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">E-mail</span>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm((atual) => ({ ...atual, email: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">
                Senha {editing ? '(opcional)' : '(obrigatória)'}
              </span>
              <input
                type="password"
                value={form.senha ?? ''}
                onChange={(e) => setForm((atual) => ({ ...atual, senha: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
                required={!editing}
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium text-gray-700">Setor</span>
              <select
                value={form.setorId}
                onChange={(e) => setForm((atual) => ({ ...atual, setorId: e.target.value }))}
                className="w-full rounded-xl border border-gray-300 px-3 py-2.5"
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

            <div className="flex flex-col justify-end gap-3 rounded-2xl border border-gray-200 px-4 py-3">
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={form.ativo}
                  onChange={(e) => setForm((atual) => ({ ...atual, ativo: e.target.checked }))}
                  className="h-4 w-4 rounded border-gray-300 text-[#21478A]"
                />
                Usuário ativo
              </label>
              <p className="text-xs text-gray-500">
                O papel administrativo é definido na seção de papéis abaixo.
              </p>
            </div>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold text-gray-900">Papéis administrativos</h2>
              <p className="text-xs text-gray-500">
                Escolha os papéis que definem privilégios administrativos do usuário.
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {(papeis.data ?? []).map((papel) => (
                <label
                  key={papel.id}
                  className={`flex items-start gap-3 rounded-2xl border px-4 py-3 ${
                    papeisSelecionados.includes(papel.nome) ? 'border-blue-300 bg-blue-50' : 'border-gray-200 bg-white'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={papeisSelecionados.includes(papel.nome)}
                    onChange={() => togglePapel(papel.nome)}
                    className="mt-1 h-4 w-4 rounded border-gray-300 text-[#21478A]"
                  />
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{formatRoleName(papel.nome)}</div>
                    <div className="text-xs text-gray-500">{papel.descricao ?? 'Sem descrição'}</div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="space-y-3">
            <div>
              <h2 className="text-sm font-semibold text-gray-900">Overrides de permissão</h2>
              <p className="text-xs text-gray-500">
                Herdar usa o baseline do setor. Liberar e negar criam exceções por usuário.
              </p>
            </div>
            <PermissionOverrideMatrix
              catalogo={catalogo.data ?? []}
              valor={overrideState}
              onChange={setOverrideState}
              disabled={catalogo.isLoading || (Boolean(editing) && (overridesUsuario.isLoading || overridesUsuario.isFetching))}
            />
          </div>

          {erro && <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

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
                className="rounded-xl border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700"
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
        colunas={[
          { chave: 'login', label: 'Login', fixo: true },
          { chave: 'nome', label: 'Nome' },
          { chave: 'email', label: 'E-mail' },
          { chave: 'setorNome', label: 'Setor' },
          {
            chave: 'papeisResumo',
            label: 'Papéis',
            formato: (valor) => <span className="max-w-xs whitespace-normal">{String(valor || 'Sem papéis')}</span>,
          },
          {
            chave: 'admin',
            label: 'Admin Plataforma',
            formato: (valor) => (
              <span className={`rounded-full px-2 py-1 text-xs font-medium ${valor ? 'bg-amber-100 text-amber-700' : 'bg-gray-100 text-gray-600'}`}>
                {valor ? 'Sim' : 'Não'}
              </span>
            ),
          },
          {
            chave: 'ativo',
            label: 'Ativo',
            formato: (valor) => (
              <span className={`rounded-full px-2 py-1 text-xs font-medium ${valor ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                {valor ? 'Sim' : 'Não'}
              </span>
            ),
          },
          {
            chave: 'permissoesResumo',
            label: 'Permissões efetivas',
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
                  className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600"
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
