import { useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, DragEvent, FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { AlertCircle, FileSpreadsheet, RefreshCcw, UploadCloud, X } from 'lucide-react';
import PermissionMatrix from './PermissionMatrix';
import AsyncMultiSelect from '../shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../shared/DataTable';
import ToastStack, { type ToastItem, type ToastTone } from '../ui/ToastStack';
import {
  useBaixarTemplateImportacaoUsuarios,
  useImportarUsuariosEmMassa,
  usePreValidarImportacaoUsuarios,
  useRevalidarImportacaoUsuarios,
} from '../../hooks/queries/useAdminUsuariosImportacao';
import {
  useCatalogoPermissoes,
  useCriarSetor,
  useSetoresAdmin,
} from '../../hooks/queries/useAdminAcesso';
import { useFiliais } from '../../hooks/queries/useDimensoes';
import type { SetorPayload } from '../../types/access';
import type {
  CreateSetorImportDraft,
  UserImportPreviewResponse,
  UserImportPreviewRow,
  UserImportResult,
} from '../../types/userImport';
import { createEmptyPermissionMap } from '../../utils/accessControl';
import { getApiErrorMessage } from '../../utils/apiError';
import { exportarRelatorioImportacaoCsv } from '../../utils/userImportReport';

const SURFACE_STYLE = {
  backgroundColor: 'var(--color-card)',
  borderColor: 'var(--color-border)',
};

const FIELD_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const PRIMARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-primary)',
  color: '#fff',
};

const SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const SOFT_PANEL_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
};

const FORM_INICIAL_SETOR: SetorPayload = {
  nome: '',
  descricao: '',
  permissoes: createEmptyPermissionMap(),
  filiaisPermitidas: [],
};

interface UsuariosImportacaoModalProps {
  open: boolean;
  onClose: () => void;
}

interface PreviewRowTable extends UserImportPreviewRow {
  mensagensResumo: string;
}

interface ResultadoErroRow {
  linha: number;
  email: string;
  motivo: string;
  tipoErro: string;
}

function formatPreviewStatus(status: UserImportPreviewRow['status']): string {
  switch (status) {
    case 'PRONTA':
      return 'Pronta';
    case 'ERRO_VALIDACAO':
      return 'Erro de validação';
    case 'CONFLITO_EMAIL_EXISTENTE':
      return 'Já existente';
    case 'SETOR_INEXISTENTE':
      return 'Setor inexistente';
    default:
      return status;
  }
}

function previewStatusStyle(status: UserImportPreviewRow['status']) {
  switch (status) {
    case 'PRONTA':
      return {
        backgroundColor: 'rgba(22, 163, 74, 0.14)',
        color: '#15803d',
      };
    case 'CONFLITO_EMAIL_EXISTENTE':
      return {
        backgroundColor: 'rgba(249, 115, 22, 0.16)',
        color: '#ea580c',
      };
    case 'SETOR_INEXISTENTE':
      return {
        backgroundColor: 'rgba(220, 38, 38, 0.14)',
        color: '#dc2626',
      };
    default:
      return {
        backgroundColor: 'rgba(71, 85, 105, 0.14)',
        color: '#475569',
      };
  }
}

function SummaryCard({ label, value, accent }: { label: string; value: number; accent: string }) {
  return (
    <div className="rounded-2xl border px-4 py-3" style={SURFACE_STYLE}>
      <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>
        {label}
      </div>
      <div className="mt-1 text-2xl font-bold" style={{ color: accent }}>
        {value}
      </div>
    </div>
  );
}

function CriarSetorInlineModal({
  setorOriginal,
  onClose,
  onCreated,
}: {
  setorOriginal: string;
  onClose: () => void;
  onCreated: (setorId: string) => void;
}) {
  const catalogo = useCatalogoPermissoes();
  const filiais = useFiliais();
  const criarSetor = useCriarSetor();
  const [form, setForm] = useState<CreateSetorImportDraft>({
    ...FORM_INICIAL_SETOR,
    nome: setorOriginal,
    nomeOriginalImportacao: setorOriginal,
  });
  const [erro, setErro] = useState('');

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErro('');

    try {
      const setorCriado = await criarSetor.mutateAsync({
        nome: form.nome,
        descricao: form.descricao,
        permissoes: form.permissoes,
        filiaisPermitidas: form.filiaisPermitidas,
      });
      onCreated(setorCriado.id);
      onClose();
    } catch (error) {
      setErro(getApiErrorMessage(error, 'Não foi possível criar o setor.'));
    }
  }

  return (
    <div className="fixed inset-0 z-[95] flex items-center justify-center bg-slate-950/45 px-4 py-6 backdrop-blur-[2px]">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 12 }}
        className="max-h-[88vh] w-full max-w-4xl overflow-y-auto rounded-3xl border p-6 shadow-2xl"
        style={SURFACE_STYLE}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="text-xl font-semibold" style={{ color: 'var(--color-text)' }}>
              Criar setor agora
            </h3>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
              Configure filiais e permissões mínimas para o setor <strong>{setorOriginal}</strong>.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border px-3 py-2 text-sm font-medium"
            style={SECONDARY_BUTTON_STYLE}
          >
            Fechar
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>Nome do setor</span>
              <input
                value={form.nome}
                onChange={(event) => setForm((current) => ({ ...current, nome: event.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
                required
              />
            </label>

            <label className="space-y-1">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>Descrição</span>
              <input
                value={form.descricao ?? ''}
                onChange={(event) => setForm((current) => ({ ...current, descricao: event.target.value }))}
                className="w-full rounded-xl border px-3 py-2.5"
                style={FIELD_STYLE}
              />
            </label>
          </div>

          <div className="space-y-3">
            <div>
              <h4 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Filiais permitidas</h4>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                O setor precisa nascer com pelo menos uma filial para manter o escopo de acesso válido.
              </p>
            </div>
            <AsyncMultiSelect
              label="Filiais"
              opcoes={filiais.data ?? []}
              selecionados={form.filiaisPermitidas}
              onChange={(filiaisPermitidas) => setForm((current) => ({ ...current, filiaisPermitidas }))}
              placeholder="Selecione ao menos uma filial"
              isLoading={filiais.isLoading}
            />
          </div>

          <div className="space-y-3">
            <div>
              <h4 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>Permissões do setor</h4>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                Defina o template mínimo que os usuários desse setor herdarão.
              </p>
            </div>
            <PermissionMatrix
              catalogo={catalogo.data ?? []}
              valor={form.permissoes}
              onChange={(permissoes) => setForm((current) => ({ ...current, permissoes }))}
              disabled={catalogo.isLoading}
            />
          </div>

          {erro && (
            <div className="rounded-xl border px-3 py-2 text-sm" style={{
              backgroundColor: '#fef2f2',
              borderColor: '#dc2626',
              color: '#991b1b',
            }}>
              {erro}
            </div>
          )}

          <div className="flex flex-wrap gap-3">
            <button
              type="submit"
              disabled={criarSetor.isPending || form.filiaisPermitidas.length === 0}
              className="rounded-xl px-4 py-2.5 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50"
              style={PRIMARY_BUTTON_STYLE}
            >
              {criarSetor.isPending ? 'Salvando setor...' : 'Salvar setor'}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border px-4 py-2.5 text-sm font-medium"
              style={SECONDARY_BUTTON_STYLE}
            >
              Cancelar
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}

export default function UsuariosImportacaoModal({ open, onClose }: UsuariosImportacaoModalProps) {
  if (!open) {
    return null;
  }

  return <UsuariosImportacaoModalAberto onClose={onClose} />;
}

function UsuariosImportacaoModalAberto({ onClose }: Pick<UsuariosImportacaoModalProps, 'onClose'>) {
  const setores = useSetoresAdmin();
  const baixarTemplate = useBaixarTemplateImportacaoUsuarios();
  const preValidacao = usePreValidarImportacaoUsuarios();
  const revalidacao = useRevalidarImportacaoUsuarios();
  const importacao = useImportarUsuariosEmMassa();

  const [preview, setPreview] = useState<UserImportPreviewResponse | null>(null);
  const [resultado, setResultado] = useState<UserImportResult | null>(null);
  const [arquivoSelecionado, setArquivoSelecionado] = useState<File | null>(null);
  const [erroLocal, setErroLocal] = useState('');
  const [resolucoesSetor, setResolucoesSetor] = useState<Record<string, string>>({});
  const [setorCriacaoAberto, setSetorCriacaoAberto] = useState<string | null>(null);
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  function pushToast(message: string, tone: ToastTone) {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    setToasts((current) => [...current, { id, message, tone }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((item) => item.id !== id));
    }, 3200);
  }

  async function processarArquivo(arquivo: File) {
    const nome = arquivo.name.toLowerCase();
    if (!nome.endsWith('.xlsx') && !nome.endsWith('.xls')) {
      setErroLocal('Formato inválido. Envie somente arquivos .xlsx ou .xls.');
      pushToast('Formato inválido para importação.', 'error');
      return;
    }

    setErroLocal('');
    setResultado(null);
    setArquivoSelecionado(arquivo);
    pushToast('Pré-validando planilha selecionada...', 'info');

    try {
      const response = await preValidacao.mutateAsync(arquivo);
      setPreview(response);
      setResolucoesSetor({});
      pushToast(
        response.setoresInexistentes.length > 0
          ? 'Planilha lida. Resolva os setores inexistentes para continuar.'
          : 'Planilha lida e pré-validada com sucesso.',
        response.setoresInexistentes.length > 0 ? 'info' : 'success',
      );
    } catch (error) {
      setPreview(null);
      setErroLocal(getApiErrorMessage(error, 'Não foi possível pré-validar a planilha.'));
      pushToast('Falha ao validar a planilha.', 'error');
    }
  }

  async function handleFileInput(event: ChangeEvent<HTMLInputElement>) {
    const arquivo = event.target.files?.[0];
    if (!arquivo) {
      return;
    }
    await processarArquivo(arquivo);
    event.target.value = '';
  }

  async function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    const arquivo = event.dataTransfer.files?.[0];
    if (!arquivo) {
      return;
    }
    await processarArquivo(arquivo);
  }

  async function handleBaixarTemplate() {
    try {
      await baixarTemplate.mutateAsync();
    } catch (error) {
      pushToast(getApiErrorMessage(error, 'Não foi possível baixar o template.'), 'error');
    }
  }

  async function handleRevalidar() {
    if (!preview) {
      return;
    }

    pushToast('Reprocessando importação com os mapeamentos selecionados...', 'info');
    try {
      const response = await revalidacao.mutateAsync({
        importacaoId: preview.importacaoId,
        resolucoesSetor: Object.entries(resolucoesSetor)
          .filter(([, setorDestinoId]) => Boolean(setorDestinoId))
          .map(([setorOriginal, setorDestinoId]) => ({ setorOriginal, setorDestinoId })),
      });
      setPreview(response);
      pushToast(
        response.setoresInexistentes.length === 0
          ? 'Importação revalidada. Agora você já pode importar.'
          : 'Ainda existem setores pendentes de resolução.',
        response.setoresInexistentes.length === 0 ? 'success' : 'info',
      );
    } catch (error) {
      pushToast(getApiErrorMessage(error, 'Não foi possível revalidar a importação.'), 'error');
    }
  }

  async function handleImportar() {
    if (!preview) {
      return;
    }

    pushToast('Importando usuários em lote...', 'info');
    try {
      const response = await importacao.mutateAsync({
        importacaoId: preview.importacaoId,
        resolucoesSetor: Object.entries(resolucoesSetor)
          .filter(([, setorDestinoId]) => Boolean(setorDestinoId))
          .map(([setorOriginal, setorDestinoId]) => ({ setorOriginal, setorDestinoId })),
      });
      setResultado(response);
      pushToast(`Importação concluída. ${response.totalCriados} usuário(s) criado(s).`, 'success');
    } catch (error) {
      pushToast(getApiErrorMessage(error, 'Não foi possível concluir a importação.'), 'error');
    }
  }

  const previewRows = useMemo<PreviewRowTable[]>(
    () =>
      (preview?.linhasPreview ?? []).map((row) => ({
        ...row,
        mensagensResumo: row.mensagens.join(' '),
      })),
    [preview],
  );

  const erroRows = useMemo<ResultadoErroRow[]>(
    () =>
      (resultado?.listaErros ?? []).map((row) => ({
        linha: row.linha,
        email: row.email ?? '—',
        motivo: row.motivo,
        tipoErro: row.tipoErro,
      })),
    [resultado],
  );

  const colunasPreview: ColunaTabela<PreviewRowTable>[] = [
    { chave: 'linha', label: 'Linha', fixo: true },
    { chave: 'nome', label: 'Nome', largura: '220px' },
    { chave: 'email', label: 'E-mail', largura: '240px' },
    { chave: 'setorOriginal', label: 'Setor no arquivo', largura: '180px' },
    { chave: 'setorResolvido', label: 'Setor aplicado', largura: '180px' },
    {
      chave: 'status',
      label: 'Status',
      largura: '180px',
      formato: (valor) => (
        <span
          className="inline-flex rounded-full px-2 py-1 text-xs font-semibold"
          style={previewStatusStyle(valor as UserImportPreviewRow['status'])}
        >
          {formatPreviewStatus(valor as UserImportPreviewRow['status'])}
        </span>
      ),
    },
    {
      chave: 'mensagensResumo',
      label: 'Mensagens',
      largura: '340px',
      formato: (valor) => <span className="whitespace-normal">{String(valor || '—')}</span>,
    },
  ];

  const colunasErros: ColunaTabela<ResultadoErroRow>[] = [
    { chave: 'linha', label: 'Linha', fixo: true },
    { chave: 'email', label: 'E-mail', largura: '240px' },
    { chave: 'tipoErro', label: 'Tipo', largura: '180px' },
    {
      chave: 'motivo',
      label: 'Motivo',
      largura: '420px',
      formato: (valor) => <span className="whitespace-normal">{String(valor)}</span>,
    },
  ];

  const faltamMapeamentos = (preview?.setoresInexistentes ?? []).some((setor) => !resolucoesSetor[setor]);

  if (typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <>
      <ToastStack items={toasts} />
      <AnimatePresence>
        <motion.div
          className="fixed inset-0 z-[80] bg-slate-950/45 backdrop-blur-[2px]"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        />

        <motion.div
          role="dialog"
          aria-modal="true"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 18 }}
          className="fixed inset-x-4 bottom-4 top-4 z-[81] mx-auto flex max-w-7xl flex-col overflow-hidden rounded-[28px] border shadow-2xl"
          style={SURFACE_STYLE}
        >
          <div className="flex items-start justify-between gap-4 border-b px-6 py-5" style={{ borderColor: 'var(--color-border)' }}>
            <div className="min-w-0">
              <div className="flex items-center gap-2 text-sm font-semibold" style={{ color: 'var(--color-primary)' }}>
                <FileSpreadsheet size={18} />
                Importação em massa de usuários
              </div>
              <h2 className="mt-1 text-2xl font-bold" style={{ color: 'var(--color-text)' }}>
                Excel com preview, resolução guiada de setores e relatório final
              </h2>
              <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                Formato obrigatório: <strong>Nome do Usuário</strong>, <strong>E-mail</strong> e <strong>Setor</strong>.
              </p>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border p-2 transition-opacity hover:opacity-80"
              style={SECONDARY_BUTTON_STYLE}
              aria-label="Fechar importação"
            >
              <X size={18} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto px-6 py-5">
            <div className="space-y-6">
              <section className="rounded-3xl border p-5" style={SOFT_PANEL_STYLE}>
                <div className="grid gap-4 lg:grid-cols-[1.6fr_auto]">
                  <div>
                    <h3 className="text-sm font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text)' }}>
                      Arquivo de entrada
                    </h3>
                    <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                      Selecione ou arraste um arquivo Excel. A pré-validação acontece antes da importação final e você pode reprocessar sem reenviar.
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() => void handleBaixarTemplate()}
                      disabled={baixarTemplate.isPending}
                      className="rounded-xl border px-4 py-2.5 text-sm font-medium disabled:opacity-50"
                      style={SECONDARY_BUTTON_STYLE}
                    >
                      {baixarTemplate.isPending ? 'Baixando...' : 'Baixar template Excel'}
                    </button>
                    <label
                      className="inline-flex cursor-pointer items-center rounded-xl px-4 py-2.5 text-sm font-semibold text-white"
                      style={PRIMARY_BUTTON_STYLE}
                    >
                      Selecionar arquivo
                      <input type="file" accept=".xlsx,.xls" className="hidden" onChange={(event) => void handleFileInput(event)} />
                    </label>
                  </div>
                </div>

                <div
                  className="mt-4 rounded-3xl border border-dashed p-6 text-center"
                  style={{
                    borderColor: 'var(--color-primary)',
                    backgroundColor: 'var(--color-bg)',
                  }}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => void handleDrop(event)}
                >
                  <UploadCloud className="mx-auto mb-3" size={24} style={{ color: 'var(--color-primary)' }} />
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
                    Arraste o Excel aqui ou use o botão de seleção
                  </p>
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                    Aceitamos arquivos `.xlsx` e `.xls`. Não existe coluna de senha.
                  </p>
                  <p className="mt-3 text-xs" style={{ color: 'var(--color-text-muted)' }}>
                    {arquivoSelecionado ? `Arquivo atual: ${arquivoSelecionado.name}` : 'Nenhum arquivo selecionado ainda.'}
                  </p>
                </div>

                {erroLocal && (
                  <div
                    className="mt-4 rounded-xl border px-3 py-2 text-sm"
                    style={{
                      backgroundColor: '#fef2f2',
                      borderColor: '#dc2626',
                      color: '#991b1b',
                    }}
                  >
                    {erroLocal}
                  </div>
                )}
              </section>

              {preview && (
                <>
                  <section className="grid gap-4 md:grid-cols-3">
                    <SummaryCard label="Total de linhas" value={preview.totais.totalLinhas} accent="var(--color-text)" />
                    <SummaryCard label="Válidas para criação" value={preview.totais.validas} accent="#10b981" />
                    <SummaryCard label="Com problemas" value={preview.totais.invalidas} accent="#ef4444" />
                  </section>

                  {preview.setoresInexistentes.length > 0 && (
                    <section className="rounded-3xl border p-5" style={SOFT_PANEL_STYLE}>
                      <div className="flex items-start gap-3">
                        <AlertCircle size={18} style={{ color: '#f97316' }} />
                        <div>
                          <h3 className="text-base font-semibold" style={{ color: 'var(--color-text)' }}>
                            Foram encontrados setores que não existem no sistema.
                          </h3>
                          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                            Resolva todos os setores abaixo criando um novo setor ou mapeando para um setor existente. Depois reprocessamos sem reenviar o Excel.
                          </p>
                        </div>
                      </div>

                      <div className="mt-4 space-y-3">
                        {preview.setoresInexistentes.map((setor) => (
                          <div key={setor} className="rounded-2xl border p-4" style={SURFACE_STYLE}>
                            <div className="flex flex-wrap items-center justify-between gap-3">
                              <div>
                                <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{setor}</div>
                                <div className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                                  Escolha um setor existente para substituir ou crie um novo agora.
                                </div>
                              </div>

                              <div className="flex flex-wrap items-center gap-3">
                                <select
                                  value={resolucoesSetor[setor] ?? ''}
                                  onChange={(event) => setResolucoesSetor((current) => ({
                                    ...current,
                                    [setor]: event.target.value,
                                  }))}
                                  className="min-w-[220px] rounded-xl border px-3 py-2 text-sm"
                                  style={FIELD_STYLE}
                                >
                                  <option value="">Mapear para setor existente</option>
                                  {(setores.data ?? []).map((setorExistente) => (
                                    <option key={setorExistente.id} value={setorExistente.id}>
                                      {setorExistente.nome}
                                    </option>
                                  ))}
                                </select>

                                <button
                                  type="button"
                                  onClick={() => setSetorCriacaoAberto(setor)}
                                  className="rounded-xl border px-3 py-2 text-sm font-medium"
                                  style={SECONDARY_BUTTON_STYLE}
                                >
                                  Criar setor agora
                                </button>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>

                      <div className="mt-4 flex flex-wrap gap-3">
                        <button
                          type="button"
                          onClick={() => void handleRevalidar()}
                          disabled={revalidacao.isPending || faltamMapeamentos}
                          className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
                          style={PRIMARY_BUTTON_STYLE}
                        >
                          <RefreshCcw size={16} />
                          {revalidacao.isPending ? 'Reprocessando...' : 'Reprocessar importação'}
                        </button>
                        <button
                          type="button"
                          onClick={onClose}
                          className="rounded-xl border px-4 py-2.5 text-sm font-medium"
                          style={SECONDARY_BUTTON_STYLE}
                        >
                          Cancelar importação
                        </button>
                      </div>
                    </section>
                  )}

                  <DataTable
                    titulo="Preview da importação"
                    dados={previewRows}
                    chaveLinha="linha"
                    colunas={colunasPreview}
                    isLoading={preValidacao.isPending || revalidacao.isPending}
                  />

                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() => void handleImportar()}
                      disabled={!preview.podeImportar || importacao.isPending}
                      className="rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
                      style={PRIMARY_BUTTON_STYLE}
                    >
                      {importacao.isPending ? 'Importando agora...' : 'Importar agora'}
                    </button>
                    <button
                      type="button"
                      onClick={() => void handleBaixarTemplate()}
                      className="rounded-xl border px-4 py-2.5 text-sm font-medium"
                      style={SECONDARY_BUTTON_STYLE}
                    >
                      Baixar template novamente
                    </button>
                  </div>
                </>
              )}

              {resultado && (
                <section className="space-y-5 rounded-3xl border p-5" style={SOFT_PANEL_STYLE}>
                  <div>
                    <h3 className="text-lg font-semibold" style={{ color: 'var(--color-text)' }}>
                      Resultado final da importação
                    </h3>
                    <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                      As senhas provisórias ficam disponíveis apenas neste relatório e no CSV exportado agora.
                    </p>
                  </div>

                  <div className="grid gap-4 md:grid-cols-3">
                    <SummaryCard label="Criados com sucesso" value={resultado.totalCriados} accent="#10b981" />
                    <SummaryCard label="Ignorados" value={resultado.totalIgnorados} accent="#f97316" />
                    <SummaryCard label="Erros" value={resultado.totalErros} accent="#ef4444" />
                  </div>

                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() => exportarRelatorioImportacaoCsv(resultado)}
                      className="rounded-xl px-4 py-2.5 text-sm font-semibold text-white"
                      style={PRIMARY_BUTTON_STYLE}
                    >
                      Exportar relatório em CSV
                    </button>
                  </div>

                  <div className="rounded-2xl border p-4" style={SURFACE_STYLE}>
                    <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
                      Credenciais temporárias geradas
                    </div>
                    <div className="mt-3 overflow-x-auto">
                      <table className="w-full min-w-[540px] text-sm">
                        <thead>
                          <tr className="border-b" style={{ borderColor: 'var(--color-border)' }}>
                            <th className="px-3 py-2 text-left text-xs uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>E-mail</th>
                            <th className="px-3 py-2 text-left text-xs uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>Senha provisória</th>
                          </tr>
                        </thead>
                        <tbody>
                          {resultado.credenciaisTemporarias.map((credencial) => (
                            <tr key={credencial.email} className="border-b last:border-b-0" style={{ borderColor: 'var(--color-border)' }}>
                              <td className="px-3 py-2" style={{ color: 'var(--color-text)' }}>{credencial.email}</td>
                              <td className="px-3 py-2 font-mono text-xs" style={{ color: 'var(--color-text)' }}>{credencial.senhaProvisoria}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <DataTable
                    titulo="Erros e motivos"
                    dados={erroRows}
                    chaveLinha="linha"
                    colunas={colunasErros}
                  />
                </section>
              )}
            </div>
          </div>
        </motion.div>
      </AnimatePresence>

      <AnimatePresence>
        {setorCriacaoAberto && (
          <CriarSetorInlineModal
            key={setorCriacaoAberto}
            setorOriginal={setorCriacaoAberto}
            onClose={() => setSetorCriacaoAberto(null)}
            onCreated={(setorId) => {
              setResolucoesSetor((current) => ({
                ...current,
                [setorCriacaoAberto]: setorId,
              }));
              pushToast(`Setor "${setorCriacaoAberto}" criado e associado à importação.`, 'success');
            }}
          />
        )}
      </AnimatePresence>
    </>,
    document.body,
  );
}
