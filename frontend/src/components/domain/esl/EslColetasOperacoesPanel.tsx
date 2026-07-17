import { useMemo, useState } from 'react';
import { CalendarDays, Edit3, Loader2, Plus, RefreshCw, XCircle } from 'lucide-react';
import DataTable, { type ColunaTabela } from '../../shared/DataTable';
import StatusBadge from '../../shared/StatusBadge';
import { useEslColetasDiarias } from '../../../hooks/queries/useEsl';
import type { EslColetaListagemItem } from '../../../types/esl';
import { dataHojeLocal } from '../../../utils/dateUtils';
import { formatarData } from '../../../utils/formatadores';
import EslCancelarColetaModal from './EslCancelarColetaModal';
import EslColetaModal from './EslColetaModal';
import EslEditarColetaModal from './EslEditarColetaModal';
import { ESL_PRIMARY_BUTTON_STYLE, ESL_SECONDARY_BUTTON_STYLE } from './EslFormControls';

interface ColetaTabelaRow extends EslColetaListagemItem {
  acoes: string;
}

function formatarDataTabela(valor: string | null): string {
  return valor ? formatarData(valor) : '—';
}

function coletaPermiteAlteracao(status: string | null): boolean {
  const normalizado = status?.trim().toLowerCase() ?? '';
  return !normalizado.includes('cancel') && normalizado !== 'canceled';
}

interface EslColetasOperacoesPanelProps {
  filialSelecionada: string | null;
  filiaisDisponiveis: string[];
  onFilialChange: (filial: string | null) => void;
}

export default function EslColetasOperacoesPanel({
  filialSelecionada,
  filiaisDisponiveis,
  onFilialChange,
}: EslColetasOperacoesPanelProps) {
  const [dataSolicitacao, setDataSolicitacao] = useState(dataHojeLocal);
  const [novaColetaAberta, setNovaColetaAberta] = useState(false);
  const [coletaEmEdicao, setColetaEmEdicao] = useState<EslColetaListagemItem | null>(null);
  const [coletaEmCancelamento, setColetaEmCancelamento] = useState<EslColetaListagemItem | null>(null);
  const filial = filialSelecionada ?? '';
  const possuiFilialSelecionada = Boolean(filialSelecionada);
  const coletas = useEslColetasDiarias(dataSolicitacao, filial, possuiFilialSelecionada);

  const rows = useMemo<ColetaTabelaRow[]>(() => (
    (coletas.data?.itens ?? []).map((item) => ({ ...item, acoes: item.coletaId }))
  ), [coletas.data?.itens]);

  const colunas = useMemo<ColunaTabela<ColetaTabelaRow>[]>(() => [
    {
      chave: 'numeroColeta',
      label: 'Coleta',
      fixo: true,
      formato: (valor, row) => valor == null ? row.coletaId : `#${Number(valor).toLocaleString('pt-BR')}`,
    },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor ?? '')} /> },
    { chave: 'dataSolicitacao', label: 'Solicitação', formato: (valor) => formatarDataTabela(valor as string | null) },
    { chave: 'dataAgendada', label: 'Agendada', formato: (valor) => formatarDataTabela(valor as string | null) },
    {
      chave: 'horaInicial',
      label: 'Janela',
      formato: (valor, row) => `${String(valor ?? '—').slice(0, 5)} – ${String(row.horaFinal ?? '—').slice(0, 5)}`,
    },
    { chave: 'referencia', label: 'Referência', largura: '180px' },
    {
      chave: 'acoes',
      label: 'Ações',
      alinhamento: 'right',
      ordenavel: false,
      formato: (_valor, row) => {
        const podeAlterar = coletaPermiteAlteracao(row.status);
        return (
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setColetaEmEdicao(row)}
              disabled={!podeAlterar}
              className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1.5 text-xs font-semibold disabled:cursor-not-allowed disabled:opacity-45"
              style={ESL_SECONDARY_BUTTON_STYLE}
              title={podeAlterar ? 'Editar coleta ESL' : 'Coleta cancelada não pode ser editada'}
            >
              <Edit3 size={13} />
              Editar
            </button>
            <button
              type="button"
              onClick={() => setColetaEmCancelamento(row)}
              disabled={!podeAlterar}
              className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1.5 text-xs font-semibold disabled:cursor-not-allowed disabled:opacity-45"
              style={{ borderColor: 'rgba(220, 38, 38, 0.45)', color: '#dc2626', backgroundColor: 'rgba(220, 38, 38, 0.08)' }}
              title={podeAlterar ? 'Cancelar coleta ESL' : 'Coleta já cancelada'}
            >
              <XCircle size={13} />
              Cancelar
            </button>
          </div>
        );
      },
    },
  ], []);

  return (
    <section className="mb-6 overflow-hidden rounded-[24px] border shadow-sm" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
      <div className="flex flex-wrap items-start justify-between gap-4 border-b px-4 py-4 sm:px-5" style={{ borderColor: 'var(--color-border)' }}>
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold" style={{ color: 'var(--color-primary)' }}>
            <CalendarDays size={18} />
            Operações ESL (Diário)
          </div>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            {possuiFilialSelecionada
              ? `Consulta remota da filial ${filialSelecionada} por data de solicitação. Não interfere nas métricas analíticas do ETL.`
              : 'Selecione uma filial operacional ao lado para consultar ou alterar operações ESL.'}
          </p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <label className="block space-y-1">
            <span className="block text-xs font-semibold" style={{ color: 'var(--color-text-muted)' }}>Filial operacional</span>
            <select
              value={filialSelecionada ?? ''}
              onChange={(event) => onFilialChange(event.target.value || null)}
              className="h-10 max-w-72 rounded-xl border px-3 text-sm outline-none focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
              style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              aria-label="Filial operacional ESL"
            >
              <option value="">Selecionar filial</option>
              {filiaisDisponiveis.map((filialDisponivel) => (
                <option key={filialDisponivel} value={filialDisponivel}>{filialDisponivel}</option>
              ))}
            </select>
          </label>
          <label className="block space-y-1">
            <span className="block text-xs font-semibold" style={{ color: 'var(--color-text-muted)' }}>Data de solicitação</span>
            <input
              type="date"
              value={dataSolicitacao}
              onChange={(event) => setDataSolicitacao(event.target.value)}
              className="h-10 rounded-xl border px-3 text-sm outline-none focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
              style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
              required
            />
          </label>
          <button
            type="button"
            onClick={() => void coletas.refetch()}
            disabled={!possuiFilialSelecionada || coletas.isFetching}
            className="inline-flex h-10 items-center gap-2 rounded-xl border px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60"
            style={ESL_SECONDARY_BUTTON_STYLE}
          >
            <RefreshCw className={coletas.isFetching ? 'animate-spin' : ''} size={15} />
            Atualizar
          </button>
          <button type="button" onClick={() => setNovaColetaAberta(true)} disabled={!possuiFilialSelecionada} className="inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60" style={ESL_PRIMARY_BUTTON_STYLE}>
            <Plus size={16} />
            Nova Coleta ESL
          </button>
        </div>
      </div>

      {coletas.isFetching && !coletas.isLoading ? (
        <div className="flex items-center gap-2 border-b px-4 py-2 text-xs" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>
          <Loader2 className="animate-spin" size={13} />
          Atualizando operações ESL…
        </div>
      ) : null}

      <div className="p-4 sm:p-5">
        {!possuiFilialSelecionada ? (
          <div className="mb-4 rounded-2xl border px-4 py-3 text-sm" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-subtle)' }}>
            As operações ESL exigem uma única filial. A seleção acima também atualiza o filtro global do dashboard.
          </div>
        ) : null}
        <DataTable
          titulo={`Coletas ESL em ${formatarData(dataSolicitacao)}`}
          dados={rows}
          colunas={colunas}
          chaveLinha="coletaId"
          isLoading={coletas.isLoading}
          error={coletas.isError ? coletas.error : undefined}
          errorFallbackMessage="Não foi possível carregar as coletas do ESL para a data selecionada."
          semPaginacao
        />
        {coletas.data?.temProximaPagina ? (
          <p className="mt-3 text-xs" style={{ color: 'var(--color-text-muted)' }}>O ESL indicou mais registros para esta data; a primeira página operacional está exibida.</p>
        ) : null}
      </div>

      <EslColetaModal filial={filial} open={novaColetaAberta} onClose={() => setNovaColetaAberta(false)} />
      <EslEditarColetaModal filial={filial} coleta={coletaEmEdicao} onClose={() => setColetaEmEdicao(null)} />
      <EslCancelarColetaModal filial={filial} coleta={coletaEmCancelamento} onClose={() => setColetaEmCancelamento(null)} />
    </section>
  );
}
