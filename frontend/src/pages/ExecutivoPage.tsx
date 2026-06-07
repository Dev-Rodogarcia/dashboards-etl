import { useEffect } from 'react';
import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import ExecutivoKpiGrid from '../components/domain/executivo/ExecutivoKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import { DATE_RANGE_PRESETS } from '../components/shared/dateRangePresets';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import MensagemErro from '../components/ui/MensagemErro';
import { salvarBlobComoArquivo } from '../api/downloadArquivo';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { useExecutivoOverview, useExecutivoResumoFinanceiro, useExecutivoSerie } from '../hooks/queries/useExecutivo';
import type { ExecutivoResumoFinanceiro } from '../types/executivo';
import { normalizarPeriodo } from '../utils/dateUtils';
import { formatarMoeda, formatarNumero } from '../utils/formatadores';

const EXECUTIVO_CHART_COLORS = {
  receitaOperacional: 'var(--color-primary)',
  valorFaturado: 'var(--color-positive-fill)',
  saldoAReceber: 'var(--color-warning-fill)',
  saldoAPagar: 'var(--color-negative-fill)',
  backlog: 'var(--color-primary)',
} as const;

type TooltipParam = {
  axisValue?: string | number;
  axisValueLabel?: string;
  marker?: string;
  seriesName?: string;
  value?: number | string | Array<number | string | null> | null;
};

function formatarMoedaEixo(valor: number | string) {
  const numero = Number(valor);
  if (!Number.isFinite(numero)) return '';
  if (Math.abs(numero) >= 1_000_000) return `${formatarMoeda(numero / 1_000_000)} mi`;
  if (Math.abs(numero) >= 1_000) return `${formatarMoeda(numero / 1_000)} mil`;
  return formatarMoeda(numero);
}

function normalizarTooltipParams(params: unknown): TooltipParam[] {
  return Array.isArray(params) ? params as TooltipParam[] : [params as TooltipParam];
}

function valorTooltip(param: TooltipParam): number {
  const valor = Array.isArray(param.value) ? param.value[param.value.length - 1] : param.value;
  const numero = Number(valor ?? 0);
  return Number.isFinite(numero) ? numero : 0;
}

function formatarTooltipFinanceiro(params: unknown) {
  const itens = normalizarTooltipParams(params);
  const titulo = itens[0]?.axisValueLabel ?? itens[0]?.axisValue ?? '';
  const linhas = itens.map((item) => `${item.marker ?? ''}${item.seriesName ?? ''}: ${formatarMoeda(valorTooltip(item))}`);
  return [titulo, ...linhas].filter(Boolean).join('<br/>');
}

function formatarTooltipExecutivoMisto(params: unknown) {
  const itens = normalizarTooltipParams(params);
  const titulo = itens[0]?.axisValueLabel ?? itens[0]?.axisValue ?? '';
  const linhas = itens.map((item) => {
    const nomeSerie = item.seriesName ?? '';
    const valor = valorTooltip(item);
    const valorFormatado = nomeSerie === 'Backlog Coletas'
      ? formatarNumero(valor)
      : formatarMoeda(valor);

    return `${item.marker ?? ''}${nomeSerie}: ${valorFormatado}`;
  });
  return [titulo, ...linhas].filter(Boolean).join('<br/>');
}

function resolveCssColor(cssColor: string) {
  if (typeof document === 'undefined') {
    return cssColor;
  }
  const match = /^var\((--[\w-]+)\)$/.exec(cssColor.trim());
  if (!match) {
    return cssColor;
  }
  return getComputedStyle(document.documentElement).getPropertyValue(match[1]).trim() || cssColor;
}

function aplicarAlpha(color: string, alpha: number) {
  const resolved = resolveCssColor(color);
  const hex = resolved.match(/^#([0-9a-f]{3}|[0-9a-f]{6})$/i)?.[1];
  if (!hex) {
    return resolved;
  }

  const normalized = hex.length === 3
    ? hex.split('').map((char) => `${char}${char}`).join('')
    : hex;
  const red = Number.parseInt(normalized.slice(0, 2), 16);
  const green = Number.parseInt(normalized.slice(2, 4), 16);
  const blue = Number.parseInt(normalized.slice(4, 6), 16);
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

function areaGradient(color: string) {
  return {
    type: 'linear' as const,
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0, color: aplicarAlpha(color, 0.28) },
      { offset: 1, color: aplicarAlpha(color, 0.03) },
    ],
  };
}

const CSV_SEPARATOR = ';';
const CSV_NEWLINE = '\r\n';
const CSV_UTF8_BOM = '\ufeff';

function protegerFormulaCsv(valor: string) {
  if (!valor) return valor;
  return ['=', '+', '-', '@'].includes(valor.charAt(0)) ? `'${valor}` : valor;
}

function formatarNumeroCsv(valor: number) {
  if (!Number.isFinite(valor)) return '';
  return valor.toLocaleString('pt-BR', {
    useGrouping: false,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function escaparValorCsv(valor: string | number | null | undefined) {
  const texto = typeof valor === 'number'
    ? formatarNumeroCsv(valor)
    : protegerFormulaCsv(valor == null ? '' : String(valor));
  const precisaAspas = texto.includes(CSV_SEPARATOR)
    || texto.includes('"')
    || texto.includes('\r')
    || texto.includes('\n')
    || /^\s|\s$/.test(texto);

  return precisaAspas ? `"${texto.replace(/"/g, '""')}"` : texto;
}

function exportarResumoFinanceiroCsv(rows: ExecutivoResumoFinanceiro[], nomeArquivo: string) {
  const headers = ['Filial', 'Total Faturado', 'Frete Peso', 'Frete Valor', 'Ticket Médio'];
  const linhas = rows.map((row) => [
    row.filial,
    row.totalFaturado,
    row.fretePeso,
    row.freteValor,
    row.ticketMedio,
  ]);
  const conteudo = [headers, ...linhas]
    .map((linha) => linha.map(escaparValorCsv).join(CSV_SEPARATOR))
    .join(CSV_NEWLINE);
  const blob = new Blob([CSV_UTF8_BOM, conteudo, CSV_NEWLINE], { type: 'text/csv;charset=utf-8' });

  salvarBlobComoArquivo(blob, nomeArquivo);
}

function ResumoFinanceiroTable({
  rows,
  isLoading,
  error,
  nomeArquivo,
}: {
  rows: ExecutivoResumoFinanceiro[];
  isLoading?: boolean;
  error?: unknown;
  nomeArquivo: string;
}) {
  const colunas: ColunaTabela<ExecutivoResumoFinanceiro>[] = [
    { chave: 'filial', label: 'Filial', fixo: true, largura: '280px' },
    {
      chave: 'totalFaturado',
      label: 'Total Faturado',
      largura: '160px',
      formato: (valor) => <span className="block text-right tabular-nums">{formatarMoeda(Number(valor ?? 0))}</span>,
    },
    {
      chave: 'fretePeso',
      label: 'Frete Peso',
      largura: '150px',
      formato: (valor) => <span className="block text-right tabular-nums">{formatarMoeda(Number(valor ?? 0))}</span>,
    },
    {
      chave: 'freteValor',
      label: 'Frete Valor',
      largura: '150px',
      formato: (valor) => <span className="block text-right tabular-nums">{formatarMoeda(Number(valor ?? 0))}</span>,
    },
    {
      chave: 'ticketMedio',
      label: 'Ticket Médio',
      largura: '150px',
      formato: (valor) => <span className="block text-right tabular-nums">{formatarMoeda(Number(valor ?? 0))}</span>,
    },
  ];

  return (
    <div className="mb-6">
      <DataTable
        titulo="Resumo financeiro por filial"
        dados={rows}
        colunas={colunas}
        chaveLinha="filial"
        isLoading={isLoading}
        error={error}
        errorFallbackMessage="Erro ao carregar resumo financeiro."
        acoesCabecalho={(
          <ExportButton
            nomeArquivo={nomeArquivo}
            onExport={!isLoading && !error ? () => exportarResumoFinanceiroCsv(rows, nomeArquivo) : undefined}
          />
        )}
      />
    </div>
  );
}

export default function ExecutivoPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();

  useEffect(() => {
    const preset180d = DATE_RANGE_PRESETS.find((preset) => preset.label === '180d');
    if (!preset180d) return;

    const range = preset180d.getRange();
    const periodo = normalizarPeriodo(range.dataInicio, range.dataFim);
    setDataRange(periodo.dataInicio, periodo.dataFim);
    // Esta excecao de periodo inicial deve rodar somente na montagem da pagina.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
  ];

  const overview = useExecutivoOverview(filtro);
  const serie = useExecutivoSerie(filtro);
  const resumoFinanceiro = useExecutivoResumoFinanceiro(filtro);

  usePageHeader({
    title: 'Executivo',
    description: 'Visão consolidada da operação, financeiro e backlog.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const serieDados = serie.data ?? [];
  const erroSerie = serie.isError ? getApiErrorMessage(serie.error, 'Erro ao carregar série executiva.') : null;
  const resumoFinanceiroDados = resumoFinanceiro.data ?? [];
  const chartColors = {
    receitaOperacional: resolveCssColor(EXECUTIVO_CHART_COLORS.receitaOperacional),
    valorFaturado: resolveCssColor(EXECUTIVO_CHART_COLORS.valorFaturado),
    saldoAReceber: resolveCssColor(EXECUTIVO_CHART_COLORS.saldoAReceber),
    saldoAPagar: resolveCssColor(EXECUTIVO_CHART_COLORS.saldoAPagar),
    backlog: resolveCssColor(EXECUTIVO_CHART_COLORS.backlog),
  };

  const financeiroOption: EChartsOption = {
    color: Object.values(chartColors),
    legend: { top: 0 },
    tooltip: { trigger: 'axis', formatter: formatarTooltipFinanceiro },
    grid: { top: 54, right: 20, bottom: 30, left: 68 },
    xAxis: { type: 'category', boundaryGap: false, data: serieDados.map((item) => item.month) },
    yAxis: { type: 'value', axisLabel: { formatter: formatarMoedaEixo } },
    series: [
      {
        name: 'Receita Operacional',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 7,
        lineStyle: { width: 2.5, color: chartColors.receitaOperacional },
        itemStyle: { color: chartColors.receitaOperacional },
        areaStyle: { color: areaGradient(EXECUTIVO_CHART_COLORS.receitaOperacional) },
        emphasis: { focus: 'series' },
        data: serieDados.map((item) => item.receitaOperacional),
      },
      {
        name: 'Valor Faturado',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 7,
        lineStyle: { width: 2.5, color: chartColors.valorFaturado },
        itemStyle: { color: chartColors.valorFaturado },
        areaStyle: { color: areaGradient(EXECUTIVO_CHART_COLORS.valorFaturado) },
        emphasis: { focus: 'series' },
        data: serieDados.map((item) => item.valorFaturado),
      },
      {
        name: 'Saldo a Receber',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 7,
        lineStyle: { width: 2.5, color: chartColors.saldoAReceber },
        itemStyle: { color: chartColors.saldoAReceber },
        areaStyle: { color: areaGradient(EXECUTIVO_CHART_COLORS.saldoAReceber) },
        emphasis: { focus: 'series' },
        data: serieDados.map((item) => item.saldoAReceber),
      },
      {
        name: 'Saldo a Pagar',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 7,
        lineStyle: { width: 2.5, color: chartColors.saldoAPagar },
        itemStyle: { color: chartColors.saldoAPagar },
        areaStyle: { color: areaGradient(EXECUTIVO_CHART_COLORS.saldoAPagar) },
        emphasis: { focus: 'series' },
        data: serieDados.map((item) => item.saldoAPagar),
      },
    ],
  };

  const backlogOption: EChartsOption = {
    color: [chartColors.valorFaturado, chartColors.backlog],
    legend: { top: 0 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' }, formatter: formatarTooltipExecutivoMisto },
    grid: { top: 54, right: 62, bottom: 32, left: 68 },
    xAxis: { type: 'category', data: serieDados.map((item) => item.month) },
    yAxis: [
      { type: 'value', name: 'Faturamento', axisLabel: { formatter: formatarMoedaEixo } },
      {
        type: 'value',
        name: 'Backlog',
        alignTicks: true,
        splitLine: { show: false },
        axisLabel: { formatter: (value: number | string) => formatarNumero(Number(value)) },
      },
    ],
    series: [
      {
        name: 'Valor Faturado',
        type: 'bar',
        yAxisIndex: 0,
        barMaxWidth: 30,
        itemStyle: { color: chartColors.valorFaturado, borderRadius: [4, 4, 0, 0] },
        data: serieDados.map((item) => item.valorFaturado),
      },
      {
        name: 'Backlog Coletas',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 2.5, color: chartColors.backlog },
        itemStyle: { color: chartColors.backlog },
        emphasis: { focus: 'series' },
        data: serieDados.map((item) => item.backlogColetas),
      },
    ],
  };

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <AsyncMultiSelect label="Filiais" opcoes={filiais.data ?? []} selecionados={filtros.filiais ?? []} onChange={(valores) => setFiltro('filiais', valores)} isLoading={filiais.isLoading} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar visão executiva.')} tipo={getTipoErro(overview.error)} />}
      {overview.isLoading && (
        <div className="mb-6 flex h-24 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent" style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
        </div>
      )}
      {!overview.isLoading && overview.data && <ExecutivoKpiGrid overview={overview.data} />}
      {!overview.isLoading && !overview.data && !overview.isError && (
        <div className="mb-6 flex h-24 items-center justify-center rounded-[20px] border text-sm" style={{ color: 'var(--color-text-muted)', backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
          Nenhum dado disponível para o período selecionado.
        </div>
      )}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper
          titulo="Tendência Financeira"
          option={financeiroOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={360}
        />
        <ChartWrapper
          titulo="Faturamento x Backlog Mensal"
          option={backlogOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={360}
        />
      </div>

      <ResumoFinanceiroTable
        rows={resumoFinanceiroDados}
        isLoading={resumoFinanceiro.isLoading}
        error={resumoFinanceiro.error}
        nomeArquivo={`executivo-resumo-financeiro-${dataInicio}-${dataFim}.csv`}
      />
    </div>
  );
}
