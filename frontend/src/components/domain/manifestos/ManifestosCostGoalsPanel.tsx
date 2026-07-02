import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { AxiosError } from 'axios';
import { Copy, Download, FileSpreadsheet, Save, Trash2 } from 'lucide-react';
import { useFiliais, useManifestosClassificacoes } from '../../../hooks/queries/useDimensoes';
import {
  useBaixarTemplateManifestosMetas,
  useDeleteManifestosMeta,
  useManifestosMetas,
  useReplicarManifestosMetasConfiguracoes,
  useSaveManifestosMeta,
} from '../../../hooks/queries/useManifestos';
import ToastStack from '../../ui/ToastStack';
import type { ToastItem, ToastTone } from '../../ui/ToastStack';
import type { ManifestosCostGoalConfig } from '../../../types/manifestos';
import { getApiErrorMessage } from '../../../utils/apiError';
import { formatarDataHora, formatarMoeda } from '../../../utils/formatadores';
import ManifestosMetasImportacaoModal from './ManifestosMetasImportacaoModal';

interface ManifestosCostGoalsPanelProps {
  open: boolean;
}

const GLOBAL_BRANCH_ID = 'GLOBAL';
const MONTHS = [
  { value: 1, label: 'Janeiro' },
  { value: 2, label: 'Fevereiro' },
  { value: 3, label: 'Março' },
  { value: 4, label: 'Abril' },
  { value: 5, label: 'Maio' },
  { value: 6, label: 'Junho' },
  { value: 7, label: 'Julho' },
  { value: 8, label: 'Agosto' },
  { value: 9, label: 'Setembro' },
  { value: 10, label: 'Outubro' },
  { value: 11, label: 'Novembro' },
  { value: 12, label: 'Dezembro' },
];
const CONTRACT_TYPE_OPTIONS = [
  { value: 'geral', label: 'Geral' },
  { value: 'frota', label: 'Frota' },
  { value: 'agregado', label: 'Agregado' },
  { value: 'terceiro', label: 'Terceiro' },
  { value: 'frota + px', label: 'Frota + PX' },
];
const GENERAL_CLASSIFICATION_KEY = '__geral__';
const FOCUS_RING_CLASS = 'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-card)]';

function normalizeCurrency(value: string) {
  const normalized = value
    .trim()
    .replace(/[^\d,.]/g, '')
    .replace(/\./g, '')
    .replace(',', '.');
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
}

function getMetasErrorMessage(error: unknown): string {
  if (error instanceof AxiosError && (!error.response || error.response.status >= 500)) {
    return 'Metas indisponíveis (API offline)';
  }

  return getApiErrorMessage(error, 'Não foi possível carregar as metas de custo operacional.');
}

function getCurrentPeriod() {
  const now = new Date();
  return {
    ano: now.getFullYear(),
    mes: now.getMonth() + 1,
  };
}

function getConfigContractTypeKey(config: ManifestosCostGoalConfig) {
  return config.contractTypeKey ?? 'geral';
}

function getConfigContractTypeLabel(config: ManifestosCostGoalConfig) {
  return config.contractType ?? CONTRACT_TYPE_OPTIONS.find((option) => option.value === getConfigContractTypeKey(config))?.label ?? 'Geral';
}

function getConfigClassificationKey(config: ManifestosCostGoalConfig) {
  return config.classificationKey ?? GENERAL_CLASSIFICATION_KEY;
}

function getConfigClassificationLabel(config: ManifestosCostGoalConfig) {
  return config.classificationKey ?? 'Geral';
}

function getClassificationPayload(value: string) {
  return value === GENERAL_CLASSIFICATION_KEY ? null : value;
}

function formatDate(year: number, month: number, day: number) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export default function ManifestosCostGoalsPanel({ open }: ManifestosCostGoalsPanelProps) {
  const periodoAtual = useMemo(() => getCurrentPeriod(), []);
  const [ano, setAno] = useState(periodoAtual.ano);
  const [mes, setMes] = useState(periodoAtual.mes);
  const [branchId, setBranchId] = useState(GLOBAL_BRANCH_ID);
  const [contractTypeKey, setContractTypeKey] = useState('geral');
  const [classificationKey, setClassificationKey] = useState(GENERAL_CLASSIFICATION_KEY);
  const [costGoalDraft, setCostGoalDraft] = useState<string | null>(null);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const filiais = useFiliais();
  const metas = useManifestosMetas(ano, mes, open);
  const isLoading = metas.isLoading;
  const error = metas.error;
  const filtroClassificacoes = useMemo(() => ({
    dataInicio: formatDate(ano, mes, 1),
    dataFim: formatDate(ano, mes, new Date(ano, mes, 0).getDate()),
  }), [ano, mes]);
  const classificacoes = useManifestosClassificacoes(filtroClassificacoes);
  const salvarMeta = useSaveManifestosMeta();
  const removerMeta = useDeleteManifestosMeta();
  const baixarTemplate = useBaixarTemplateManifestosMetas();
  const replicarMetas = useReplicarManifestosMetasConfiguracoes();
  const isSaving = salvarMeta.isPending || removerMeta.isPending || baixarTemplate.isPending || replicarMetas.isPending;
  const saveError = salvarMeta.error ?? removerMeta.error ?? baixarTemplate.error;
  const configs = useMemo(() => metas.data ?? [], [metas.data]);
  const configsCadastradas = useMemo(
    () => configs.filter((item) => item.configurado !== false),
    [configs],
  );
  const statusMensagem = useMemo(
    () => configs.find((item) => item.configurado === false)?.mensagem ?? null,
    [configs],
  );
  const contractTypeOptions = useMemo(() => {
    const options = new Map(CONTRACT_TYPE_OPTIONS.map((option) => [option.value, option]));
    configsCadastradas.forEach((config) => {
      const key = getConfigContractTypeKey(config);
      if (!options.has(key)) {
        options.set(key, { value: key, label: getConfigContractTypeLabel(config) });
      }
    });
    return Array.from(options.values());
  }, [configsCadastradas]);
  const classificationOptions = useMemo(() => {
    const options = new Map<string, { value: string; label: string }>();
    options.set(GENERAL_CLASSIFICATION_KEY, { value: GENERAL_CLASSIFICATION_KEY, label: 'Geral' });

    (classificacoes.data ?? []).forEach((classificacao) => {
      const value = classificacao.trim().toLowerCase();
      if (value && !options.has(value)) {
        options.set(value, { value, label: classificacao });
      }
    });

    configsCadastradas.forEach((config) => {
      const value = getConfigClassificationKey(config);
      if (!options.has(value)) {
        options.set(value, { value, label: getConfigClassificationLabel(config) });
      }
    });

    return Array.from(options.values());
  }, [classificacoes.data, configsCadastradas]);
  const configAtual = useMemo(
    () => configsCadastradas.find((item) =>
      item.branchId === branchId
      && getConfigContractTypeKey(item) === contractTypeKey
      && getConfigClassificationKey(item) === classificationKey
    ) ?? null,
    [branchId, classificationKey, contractTypeKey, configsCadastradas],
  );
  const selectableBranches = useMemo(
    () => [
      GLOBAL_BRANCH_ID,
      ...Array.from(new Set([
        ...(filiais.data ?? []),
        ...configsCadastradas.map((item) => item.branchId),
      ]))
        .filter((item) => item && item !== GLOBAL_BRANCH_ID)
        .sort((left, right) => left.localeCompare(right, 'pt-BR')),
    ],
    [configsCadastradas, filiais.data],
  );
  const anos = useMemo(() => {
    const current = new Date().getFullYear();
    return Array.from({ length: 7 }, (_, index) => current - 2 + index);
  }, []);
  const selectedContractType = contractTypeOptions.find((option) => option.value === contractTypeKey) ?? CONTRACT_TYPE_OPTIONS[0];
  const selectedClassification = classificationOptions.find((option) => option.value === classificationKey)
    ?? classificationOptions[0]
    ?? { value: GENERAL_CLASSIFICATION_KEY, label: 'Geral' };
  const costGoalValue = costGoalDraft ?? String(configAtual?.costGoal ?? 0);
  const podeReplicarMetas = configsCadastradas.length === 0 && !isLoading && !error;
  const actionError = saveError ?? replicarMetas.error;

  function pushToast(message: string, tone: ToastTone) {
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((current) => [...current, { id, message, tone }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((item) => item.id !== id));
    }, 4500);
  }

  if (!open) {
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await salvarMeta.mutateAsync({
      branchId,
      contractType: selectedContractType.label,
      contractTypeKey: selectedContractType.value,
      classificationKey: getClassificationPayload(selectedClassification.value),
      ano,
      mes,
      costGoal: normalizeCurrency(costGoalValue),
    });
    setCostGoalDraft(null);
  }

  async function handleRemove(config: ManifestosCostGoalConfig) {
    await removerMeta.mutateAsync({
      branchId: config.branchId,
      contractTypeKey: getConfigContractTypeKey(config),
      classificationKey: config.classificationKey ?? null,
      ano: config.ano,
      mes: config.mes,
    });
    if (
      config.branchId === branchId
      && getConfigContractTypeKey(config) === contractTypeKey
      && getConfigClassificationKey(config) === classificationKey
    ) {
      setCostGoalDraft(null);
    }
  }

  async function handleBaixarTemplate() {
    await baixarTemplate.mutateAsync();
  }

  async function handleReplicarMetas() {
    if (!podeReplicarMetas) {
      return;
    }

    try {
      const response = await replicarMetas.mutateAsync({ ano, mes });
      setCostGoalDraft(null);
      const total = response.filter((item) => item.configurado !== false).length;
      pushToast(`${total} meta${total === 1 ? '' : 's'} copiada${total === 1 ? '' : 's'} do mês anterior.`, 'success');
    } catch (err) {
      pushToast(getApiErrorMessage(err, 'Não foi possível copiar as metas do mês anterior.'), 'error');
    }
  }

  return (
    <>
      <section
        className="mb-4 rounded-[20px] border p-5 shadow-sm"
        style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
        aria-label="Gerenciamento de orçamento de custo operacional"
      >
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold" style={{ color: 'var(--color-text)' }}>
            Gerenciar Orçamento de Custo Operacional
          </h2>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            Metas mensais de custo operacional por filial, tipo de contrato e classificação.
          </p>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <div className="flex min-w-max flex-shrink-0 items-end">
            <button
              type="button"
              onClick={() => void handleReplicarMetas()}
              disabled={!podeReplicarMetas || replicarMetas.isPending}
              title={configsCadastradas.length > 0 ? 'Disponível apenas quando o mês selecionado ainda não possui metas.' : undefined}
              className={`inline-flex h-11 min-w-max flex-shrink-0 shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-xl border px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${FOCUS_RING_CLASS}`}
              style={{
                backgroundColor: podeReplicarMetas ? 'rgba(37, 99, 235, 0.10)' : 'var(--color-bg)',
                borderColor: podeReplicarMetas ? 'var(--color-primary)' : 'var(--color-border)',
                color: podeReplicarMetas ? 'var(--color-primary)' : 'var(--color-text-muted)',
              }}
            >
              <Copy size={15} />
              {replicarMetas.isPending ? 'Copiando...' : 'Copiar Metas do Mês Anterior'}
            </button>
          </div>
          <button
            type="button"
            onClick={() => setIsImportModalOpen(true)}
            className={`inline-flex h-10 items-center gap-2 rounded-xl border px-3 text-sm font-semibold ${FOCUS_RING_CLASS}`}
            style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            <FileSpreadsheet size={15} />
            Importar Planilha de Metas
          </button>
          <button
            type="button"
            onClick={() => void handleBaixarTemplate()}
            disabled={baixarTemplate.isPending}
            className={`inline-flex h-10 items-center gap-2 rounded-xl border px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${FOCUS_RING_CLASS}`}
            style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            <Download size={15} />
            {baixarTemplate.isPending ? 'Baixando...' : 'Baixar Planilha Modelo'}
          </button>
          {isLoading ? (
            <span className="rounded-full px-3 py-1 text-xs font-semibold" style={{ backgroundColor: 'rgba(37, 99, 235, 0.12)', color: '#1d4ed8' }}>
              Carregando metas
            </span>
          ) : null}
        </div>
      </div>

          {error ? (
            <p className="mb-4 rounded-xl border px-3 py-2 text-sm" style={{ borderColor: '#dc2626', color: '#dc2626', backgroundColor: 'rgba(220, 38, 38, 0.08)' }}>
              {getMetasErrorMessage(error)}
            </p>
          ) : null}

          {!error && statusMensagem ? (
            <p className="mb-4 rounded-xl border px-3 py-2 text-sm" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)', backgroundColor: 'var(--color-bg)' }}>
              {statusMensagem}
            </p>
          ) : null}

          {actionError ? (
            <p className="mb-4 rounded-xl border px-3 py-2 text-sm" style={{ borderColor: '#dc2626', color: '#dc2626', backgroundColor: 'rgba(220, 38, 38, 0.08)' }}>
              {getApiErrorMessage(actionError, 'Não foi possível salvar a meta.')}
            </p>
          ) : null}

          <form onSubmit={handleSubmit} className="grid gap-4 rounded-2xl border p-4 lg:grid-cols-[0.75fr_1fr_1.2fr_1.2fr_1.2fr_1fr_auto]" style={{ borderColor: 'var(--color-border)' }}>
            <label className="grid gap-1 text-sm font-semibold" style={{ color: 'var(--color-text-subtle)' }}>
              Ano
              <select
                value={ano}
                onChange={(event) => {
                  setCostGoalDraft(null);
                  setAno(Number(event.target.value));
                }}
                className={`h-11 rounded-xl border px-3 text-sm ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              >
                {anos.map((year) => <option key={year} value={year}>{year}</option>)}
              </select>
            </label>
            <label className="grid gap-1 text-sm font-semibold" style={{ color: 'var(--color-text-subtle)' }}>
              Mês
              <select
                value={mes}
                onChange={(event) => {
                  setCostGoalDraft(null);
                  setMes(Number(event.target.value));
                }}
                className={`h-11 rounded-xl border px-3 text-sm ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              >
                {MONTHS.map((month) => <option key={month.value} value={month.value}>{month.label}</option>)}
              </select>
            </label>
            <label className="grid gap-1 text-sm font-semibold" style={{ color: 'var(--color-text-subtle)' }}>
              Filial
              <select
                value={branchId}
                onChange={(event) => {
                  setBranchId(event.target.value);
                  setCostGoalDraft(null);
                }}
                className={`h-11 rounded-xl border px-3 text-sm ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              >
                {selectableBranches.map((option) => (
                  <option key={option} value={option}>{option === GLOBAL_BRANCH_ID ? 'GLOBAL' : option}</option>
                ))}
              </select>
            </label>
            <label className="grid gap-1 text-sm font-semibold" style={{ color: 'var(--color-text-subtle)' }}>
              Tipo de Contrato
              <select
                value={contractTypeKey}
                onChange={(event) => {
                  setContractTypeKey(event.target.value);
                  setCostGoalDraft(null);
                }}
                className={`h-11 rounded-xl border px-3 text-sm ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              >
                {contractTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="grid gap-1 text-sm font-semibold" style={{ color: 'var(--color-text-subtle)' }}>
              Classificação
              <select
                value={classificationKey}
                onChange={(event) => {
                  setClassificationKey(event.target.value);
                  setCostGoalDraft(null);
                }}
                className={`h-11 rounded-xl border px-3 text-sm ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              >
                {classificationOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="grid gap-1 text-sm font-semibold" style={{ color: 'var(--color-text-subtle)' }}>
              Meta Mensal
              <input
                type="text"
                inputMode="decimal"
                value={costGoalValue}
                onChange={(event) => setCostGoalDraft(event.target.value)}
                placeholder="299.000,00"
                autoComplete="off"
                className={`h-11 rounded-xl border px-3 text-sm tabular-nums ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              />
            </label>
            <div className="flex items-end">
              <button
                type="submit"
                disabled={isSaving}
                className={`inline-flex h-11 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50 ${FOCUS_RING_CLASS}`}
                style={{ backgroundColor: 'var(--color-primary)' }}
              >
                <Save size={15} />
                {isSaving ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          </form>

          <div className="mt-4 rounded-2xl border p-4" style={{ borderColor: 'var(--color-border)' }}>
            <h3 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
              Orçamentos cadastrados para {MONTHS.find((item) => item.value === mes)?.label}/{ano}
            </h3>
            <div className="mt-3 grid gap-2">
              {configsCadastradas.length === 0 ? (
                <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>
                  {statusMensagem ?? 'Nenhuma meta cadastrada para este mês.'}
                </p>
              ) : (
                configsCadastradas.map((config) => (
                  <div key={`${config.branchId}-${config.ano}-${config.mes}-${getConfigContractTypeKey(config)}-${getConfigClassificationKey(config)}`} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border px-3 py-3" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-bg)' }}>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold" style={{ color: 'var(--color-text)' }}>{config.branchId}</span>
                        <span className="rounded-full border px-2 py-0.5 text-[11px] font-semibold" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>
                          {getConfigContractTypeLabel(config)}
                        </span>
                        <span className="rounded-full border px-2 py-0.5 text-[11px] font-semibold" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>
                          {getConfigClassificationLabel(config)}
                        </span>
                        <span className="rounded-full border px-2 py-0.5 text-[11px] font-semibold" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>
                          Meta Mensal {formatarMoeda(config.costGoal)}
                        </span>
                      </div>
                      <p className="mt-1 text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                        Última atualização: {config.updatedAt ? formatarDataHora(config.updatedAt) : '—'} · {config.updatedByName ?? 'Usuário não identificado'}
                      </p>
                    </div>
                    <button
                      type="button"
                      disabled={isSaving}
                      onClick={() => void handleRemove(config)}
                      className={`inline-flex items-center gap-2 rounded-lg border px-3 py-1.5 text-xs font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${FOCUS_RING_CLASS}`}
                      style={{ borderColor: '#dc2626', color: '#dc2626' }}
                    >
                      <Trash2 size={13} />
                      Remover
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
      </section>
      <ManifestosMetasImportacaoModal open={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
      <ToastStack items={toasts} />
    </>
  );
}
