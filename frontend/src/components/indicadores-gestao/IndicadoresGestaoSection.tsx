import type { ReactNode } from 'react';
import type { EChartsOption } from 'echarts';
import { ChevronDown, ChevronUp } from 'lucide-react';
import ChartWrapper from '../charts/ChartWrapper';
import DataTable, { type ColunaTabela } from '../shared/DataTable';
import ExportButton from '../shared/ExportButton';
import KpiCard from '../shared/KpiCard';
import KpiGrid from '../shared/KpiGrid';
import MensagemErro from '../ui/MensagemErro';
import { getApiErrorMessage, getTipoErro } from '../../utils/apiError';
import { getGoalToneStyle, type GoalTone } from '../../utils/indicadoresGestaoVistaUi';

interface GoalBadgeProps {
  label: string;
  tone?: GoalTone;
}

function GoalBadge({ label, tone = 'neutral' }: GoalBadgeProps) {
  const style = getGoalToneStyle(tone);

  return (
    <span
      className="inline-flex rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-wide"
      style={{ backgroundColor: style.badgeBg, color: style.badgeText }}
    >
      {label}
    </span>
  );
}

interface IndicadoresGestaoSectionProps<T> {
  title: string;
  description: string;
  goalLabel: string;
  goalTone?: GoalTone;
  error?: unknown;
  goalOverridesNotice?: ReactNode;
  alert?: ReactNode;
  extra?: ReactNode;
  kpis: Array<{ label: string; value: string; icon?: ReactNode; tone?: GoalTone; progressPct?: number | null }>;
  chartTitle: string;
  chartOption: EChartsOption;
  chartLoading: boolean;
  chartEmpty: boolean;
  chartError?: string | null;
  exportName: string;
  onExport?: () => Promise<void> | void;
  tableTitle: string;
  tableData: T[];
  tableColumns: ColunaTabela<T>[];
  rowKey: keyof T & string;
  tableLoading: boolean;
  tableTotal?: number;
  tablePage?: number;
  tablePageSize?: number;
  onTablePageChange?: (pagina: number) => void;
  onTablePageSizeChange?: (tamanhoPagina: number) => void;
  isExpanded: boolean;
  onToggleTable: () => void;
}

export default function IndicadoresGestaoSection<T>({
  title,
  description,
  goalLabel,
  goalTone = 'neutral',
  error,
  goalOverridesNotice,
  alert,
  extra,
  kpis,
  chartTitle,
  chartOption,
  chartLoading,
  chartEmpty,
  chartError,
  exportName,
  onExport,
  tableTitle,
  tableData,
  tableColumns,
  rowKey,
  tableLoading,
  tableTotal,
  tablePage,
  tablePageSize,
  onTablePageChange,
  onTablePageSizeChange,
  isExpanded,
  onToggleTable,
}: IndicadoresGestaoSectionProps<T>) {
  const totalTabela = tableTotal ?? tableData.length;
  const resumoTabela = tableTotal == null
    ? `${tableData.length} registros carregados`
    : `${totalTabela} registros encontrados`;

  return (
    <section
      className="mb-8 rounded-[24px] border p-5 shadow-sm"
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
    >
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold" style={{ color: 'var(--color-text)' }}>
              {title}
            </h2>
            <GoalBadge label={goalLabel} tone={goalTone} />
          </div>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            {description}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <ExportButton dados={tableData as unknown as Record<string, unknown>[]} nomeArquivo={exportName} onExport={onExport} />
          <button
            type="button"
            onClick={onToggleTable}
            className="inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-xs font-medium transition-colors"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            {isExpanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
            {isExpanded ? 'Ocultar tabela' : 'Mostrar tabela'}
          </button>
        </div>
      </div>

      {Boolean(error) && (
        <MensagemErro mensagem={getApiErrorMessage(error, `Erro ao carregar ${title}.`)} tipo={getTipoErro(error)} />
      )}
      {goalOverridesNotice ? <div className="mb-4">{goalOverridesNotice}</div> : null}
      {alert}
      {extra}

      <div className="mb-5">
        <KpiGrid count={4}>
          {kpis.map((kpi) => (
            <KpiCard
              key={kpi.label}
              label={kpi.label}
              valor={kpi.value}
              icone={kpi.icon}
              tone={kpi.tone ?? goalTone}
              progressPct={kpi.progressPct}
            />
          ))}
        </KpiGrid>
      </div>

      <div className="mb-4">
        <ChartWrapper
          titulo={chartTitle}
          option={chartOption}
          isLoading={chartLoading}
          isEmpty={chartEmpty}
          erro={chartError}
          emptyMessage="Sem dados para o período selecionado."
          altura={320}
        />
      </div>

      <div
        className="overflow-hidden rounded-[20px] border"
        style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-bg)' }}
      >
        <div
          className="flex flex-wrap items-center justify-between gap-3 px-4 py-3"
          style={{ borderBottom: isExpanded ? '1px solid var(--color-border)' : 'none' }}
        >
          <div>
            <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
              {tableTitle}
            </div>
            <div className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              {resumoTabela}
            </div>
          </div>
          <div className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
            {isExpanded ? 'Tabela expandida' : 'Tabela recolhida por padrao'}
          </div>
        </div>

        {isExpanded ? (
          <div className="p-3">
            <DataTable
              titulo={tableTitle}
              dados={tableData}
              colunas={tableColumns}
              chaveLinha={rowKey}
              isLoading={tableLoading}
              mostrarCabecalho={false}
              totalRegistros={tableTotal}
              paginaAtual={tablePage}
              tamanhoPagina={tablePageSize}
              onPaginaChange={onTablePageChange}
              onTamanhoPaginaChange={onTablePageSizeChange}
            />
          </div>
        ) : null}
      </div>
    </section>
  );
}
