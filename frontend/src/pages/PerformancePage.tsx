import { useCallback, useEffect, useMemo, useState } from 'react';
import type { EChartsOption } from 'echarts';
import { ChevronRight, ChevronUp } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import ChartWrapper from '../components/charts/ChartWrapper';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DateRangePicker from '../components/shared/DateRangePicker';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import KpiCard from '../components/shared/KpiCard';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import {
  usePerformanceAging,
  usePerformanceDrilldown,
  usePerformanceHistorico,
  usePerformanceOverview,
  usePerformanceSerieTemporal,
  usePerformanceStatus,
} from '../hooks/queries/usePerformance';
import { usePerformanceData } from '../hooks/usePerformanceData';
import type {
  PerformanceAgingPoint,
  PerformanceDrilldownNivel,
  PerformanceDrilldownPoint,
  PerformanceFiltro,
  PerformanceHistoricoPoint,
  PerformanceSerieTemporalPoint,
  PerformanceStatusDistribuicao,
  PerformanceTempoNivel,
} from '../types/performance';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { CORES, CORES_STATUS, PALETA_SERIES } from '../utils/chartColors';
import { formatarNumero, formatarPorcentagem } from '../utils/formatadores';

const STATUS_OPTIONS = ['Pendente', 'Em Trânsito', 'Finalizada', 'Cancelada', 'Em Tratativa'];
const PERFORMANCE_EMPTY_MESSAGE = 'Nenhum dado encontrado para o período selecionado.';
const NIVEL_PARAM = 'performanceNivel';
const ANO_PARAM = 'performanceAno';
const MES_PARAM = 'performanceMes';
const NIVEIS_TEMPORAIS: Array<{ valor: PerformanceTempoNivel; label: string }> = [
  { valor: 'ano', label: 'Ano' },
  { valor: 'mes', label: 'Mês' },
  { valor: 'dia', label: 'Dia' },
];

function chartClickName(params: unknown): string | null {
  const item = params as { name?: unknown; data?: { name?: unknown } };
  if (typeof item.data?.name === 'string') {
    return item.data.name;
  }
  return typeof item.name === 'string' ? item.name : null;
}

function truncateLabel(value: string, maxLength: number): string {
  return value.length > maxLength ? `${value.slice(0, maxLength - 3)}...` : value;
}

function formatMonthLabel(value: string): string {
  const [year, month] = value.split('-');
  if (!year || !month) {
    return value;
  }
  return `${month}/${year}`;
}

function normalizeTemporalNivel(value: string | null): PerformanceTempoNivel {
  return value === 'ano' || value === 'mes' || value === 'dia' ? value : 'dia';
}

function numeroParam(valor: string | null): number | null {
  if (!valor) return null;
  const parsed = Number.parseInt(valor, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function formatarLabelTemporal(data: string, nivel: PerformanceTempoNivel): string {
  const [ano, mes, dia] = data.split('-');
  if (nivel === 'ano') return ano;
  if (nivel === 'mes') return `${mes}/${ano}`;
  return `${dia}/${mes}`;
}

function monthName(value: string): string {
  const [year, month] = value.split('-');
  const date = new Date(Number(year), Number(month) - 1, 1);
  if (Number.isNaN(date.getTime())) {
    return formatMonthLabel(value);
  }
  return new Intl.DateTimeFormat('pt-BR', { month: 'long' }).format(date);
}

function splitFiltroTexto(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function joinFiltroTexto(values?: string[]): string {
  return values?.join(', ') ?? '';
}

function normalizeDrillNivel(value: string | null): PerformanceDrilldownNivel {
  if (value === 'regiao' || value === 'cidade') {
    return value;
  }
  return 'responsavel';
}

function buildSerieTemporalOption(dados: PerformanceSerieTemporalPoint[], nivel: PerformanceTempoNivel): EChartsOption {
  return {
    legend: { top: 0 },
    grid: { top: 42, left: 10, right: 10, bottom: 10, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dados.map((item) => item.date),
      axisLabel: {
        formatter: (value: string) => formatarLabelTemporal(value, nivel),
      },
    },
    yAxis: { type: 'value', name: 'Qtd' },
    series: [
      {
        name: 'Finalizadas',
        type: 'line',
        stack: 'total',
        areaStyle: {},
        smooth: true,
        data: dados.map((item) => item.finalizadas),
        itemStyle: { color: CORES_STATUS['finalizado'] },
      },
      {
        name: 'Em Trânsito',
        type: 'line',
        stack: 'total',
        areaStyle: {},
        smooth: true,
        data: dados.map((item) => item.emTransito ?? 0),
        itemStyle: { color: CORES_STATUS['em trânsito'] },
      },
      {
        name: 'Pendente',
        type: 'line',
        stack: 'total',
        areaStyle: {},
        smooth: true,
        data: dados.map((item) => item.pendentes ?? 0),
        itemStyle: { color: CORES_STATUS['pendente'] },
      },
      {
        name: 'Canceladas',
        type: 'line',
        stack: 'total',
        areaStyle: {},
        smooth: true,
        data: dados.map((item) => item.canceladas),
        itemStyle: { color: CORES_STATUS['cancelada'] },
      },
      {
        name: 'Em Tratativa',
        type: 'line',
        stack: 'total',
        areaStyle: {},
        smooth: true,
        data: dados.map((item) => item.emTratativa),
        itemStyle: { color: CORES_STATUS['em tratativa'] },
      },
    ],
  };
}

function buildStatusOption(dados: PerformanceStatusDistribuicao[]): EChartsOption {
  return {
    grid: { top: 24, right: 18, bottom: 38, left: 34, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: dados.map((item) => item.status),
      axisLabel: {
        interval: 0,
        formatter: (value: string) => truncateLabel(value, 14),
      },
    },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: dados.map((item, index) => ({
          name: item.status,
          value: item.total,
          itemStyle: { color: PALETA_SERIES[index % PALETA_SERIES.length], borderRadius: [4, 4, 0, 0] },
        })),
        barMaxWidth: 46,
      },
    ],
  };
}

function buildHistoricoOption(dados: PerformanceHistoricoPoint[]): EChartsOption {
  const percentuais = dados.flatMap((item) => [item.performancePercentual, item.metaPercentual]);
  const menor = percentuais.length > 0 ? Math.min(...percentuais) : 0;
  const maior = percentuais.length > 0 ? Math.max(...percentuais) : 100;
  const yMin = Math.max(0, Math.floor(menor - 3));
  const yMax = Math.min(100, Math.ceil(maior + 3));

  return {
    title: {
      text: 'Histórico da Performance de Entregas',
      left: 24,
      top: 8,
      textStyle: {
        fontSize: 12,
        fontWeight: 500,
        color: 'var(--color-text)',
      },
    },
    grid: { top: 58, right: 24, bottom: 34, left: 34, containLabel: true },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const entries = params as { marker?: string; seriesName: string; value: number; name: string }[];
        const name = entries[0]?.name ?? '';
        return [
          `<strong>${name}</strong>`,
          ...entries.map((item) => `${item.marker ?? ''}${item.seriesName}: ${formatarPorcentagem(Number(item.value ?? 0), 2)}`),
        ].join('<br/>');
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dados.map((item) => item.date),
      axisLabel: {
        formatter: monthName,
        fontWeight: 600,
      },
    },
    yAxis: {
      type: 'value',
      min: yMin,
      max: yMax <= yMin ? yMin + 10 : yMax,
      splitLine: {
        lineStyle: { type: 'dashed', color: 'rgba(100, 116, 139, 0.32)' },
      },
      axisLabel: { formatter: (value: number) => `${value}%` },
    },
    series: [
      {
        name: 'Performance',
        type: 'line',
        smooth: true,
        data: dados.map((item) => item.performancePercentual),
        symbol: 'circle',
        symbolSize: 6,
        label: {
          show: true,
          position: 'bottom',
          formatter: (params: unknown) => {
            const item = params as { value?: unknown };
            return formatarPorcentagem(Number(item.value ?? 0), 1);
          },
          color: 'var(--color-text)',
          fontSize: 10,
          fontWeight: 700,
        },
        lineStyle: { width: 3, color: CORES.primaria },
        itemStyle: { color: CORES.primaria },
      },
      {
        name: 'Meta',
        type: 'line',
        data: dados.map((item) => item.metaPercentual),
        symbol: 'none',
        lineStyle: { width: 2, type: 'dashed', color: 'rgba(71, 85, 105, 0.75)' },
        itemStyle: { color: 'rgba(71, 85, 105, 0.75)' },
      },
    ],
  };
}

function buildDrilldownOption(dados: PerformanceDrilldownPoint[], nivel: PerformanceDrilldownNivel): EChartsOption {
  const labels = dados.map((item) => item.nome);
  const axisRotate = labels.some((label) => label.length > 14) ? 24 : 0;
  return {
    legend: { top: 0 },
    grid: { top: 52, right: 22, bottom: 44, left: 40, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: labels.map((item) => truncateLabel(item, 18)),
      axisLabel: {
        interval: 0,
        rotate: axisRotate,
      },
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'FORA DO PRAZO',
        type: 'bar',
        stack: 'performance',
        data: dados.map((item) => ({ name: item.nome, value: item.foraDoPrazo })),
        itemStyle: { color: CORES.perigo },
        cursor: nivel === 'cidade' ? 'default' : 'pointer',
      },
      {
        name: 'NO PRAZO',
        type: 'bar',
        stack: 'performance',
        data: dados.map((item) => ({ name: item.nome, value: item.noPrazo })),
        itemStyle: { color: CORES.sucesso },
        cursor: nivel === 'cidade' ? 'default' : 'pointer',
      },
      {
        name: 'EM ATRASO',
        type: 'bar',
        stack: 'performance',
        data: dados.map((item) => ({ name: item.nome, value: item.emAtraso })),
        itemStyle: { color: CORES.aviso },
        cursor: nivel === 'cidade' ? 'default' : 'pointer',
      },
    ],
  };
}

function buildAgingOption(dados: PerformanceAgingPoint[]): EChartsOption {
  const ordem = ['0-2 dias', '3-5 dias', '6-10 dias', '11+ dias'];
  const porBucket = new Map(dados.map((item) => [item.bucket, item.total]));
  return {
    grid: { top: 24, right: 18, bottom: 32, left: 34, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: ordem },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: ordem.map((bucket) => porBucket.get(bucket) ?? 0),
        barMaxWidth: 72,
        itemStyle: { color: CORES.aviso, borderRadius: [4, 4, 0, 0] },
      },
    ],
  };
}

function TextListFilter({
  label,
  values,
  onChange,
}: {
  label: string;
  values?: string[];
  onChange: (values: string[]) => void;
}) {
  const [rawValue, setRawValue] = useState(joinFiltroTexto(values));
  const valuesKey = joinFiltroTexto(values);

  useEffect(() => {
    setRawValue(valuesKey);
  }, [valuesKey]);

  function commitValue() {
    onChange(splitFiltroTexto(rawValue));
  }

  return (
    <label className="flex w-full min-w-[180px] flex-col gap-1 self-start md:w-56">
      <span className="text-xs font-medium leading-4" style={{ color: 'var(--color-text-muted)' }}>
        {label}
      </span>
      <input
        type="text"
        value={rawValue}
        onChange={(event) => setRawValue(event.target.value)}
        onBlur={commitValue}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            commitValue();
          }
        }}
        className="h-10 rounded-lg border px-3 text-sm shadow-sm outline-none transition-all duration-150 focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]"
        style={{
          backgroundColor: 'var(--color-card)',
          borderColor: 'var(--color-border)',
          color: 'var(--color-text)',
        }}
      />
    </label>
  );
}

function PerformanceKpiSkeleton() {
  return (
    <div
      className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-12"
      aria-hidden="true"
    >
      {Array.from({ length: 9 }).map((_, index) => (
        <div
          key={index}
          className={`${index >= 6 ? 'xl:col-span-2' : 'xl:col-span-1'} h-[102px] animate-pulse rounded-lg border p-3`}
          style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
        >
          <div className="mb-4 h-3 w-2/3 rounded bg-slate-200" />
          <div className="h-8 w-1/2 rounded bg-slate-200" />
          <div className="mt-3 h-2 w-3/4 rounded bg-slate-100" />
        </div>
      ))}
    </div>
  );
}

function kpiGridSpan(label: string): string {
  return label === 'Peso Taxado (t)' || label === 'Comprovante Anexado' || label === 'Valor NF sem Comprovante'
    ? 'xl:col-span-2'
    : 'xl:col-span-1';
}

function kpiValorClassName(label: string): string | undefined {
  if (label === 'Valor NF sem Comprovante') {
    return 'text-[clamp(1rem,1.2vw,1.35rem)] font-bold whitespace-nowrap';
  }
  if (label === 'Comprovante Anexado' || label === 'Performance') {
    return 'text-xl font-bold truncate';
  }
  return undefined;
}

function DrilldownActions({
  nivel,
  responsavel,
  regiao,
  onBack,
}: {
  nivel: PerformanceDrilldownNivel;
  responsavel: string | null;
  regiao: string | null;
  onBack: () => void;
}) {
  return (
    <div className="flex min-w-0 flex-wrap items-center justify-end gap-1 text-[11px] font-semibold">
      <button
        type="button"
        title="Drill up"
        aria-label="Drill up"
        disabled={nivel === 'responsavel'}
        onClick={onBack}
        className="flex h-7 w-7 items-center justify-center rounded-md border transition disabled:cursor-not-allowed disabled:opacity-35"
        style={{ borderColor: 'var(--color-border)', color: nivel === 'responsavel' ? 'var(--color-text-muted)' : CORES.primaria }}
      >
        <ChevronUp size={14} />
      </button>
      <span style={{ color: nivel === 'responsavel' ? CORES.primaria : 'var(--color-text-muted)' }}>
        Responsável
      </span>
      <ChevronRight size={12} style={{ color: 'var(--color-text-subtle)' }} />
      <span
        className="max-w-32 truncate"
        title={responsavel ?? undefined}
        style={{ color: nivel === 'regiao' ? CORES.primaria : 'var(--color-text-muted)' }}
      >
        Região
      </span>
      <ChevronRight size={12} style={{ color: 'var(--color-text-subtle)' }} />
      <span
        className="max-w-32 truncate"
        title={regiao ?? undefined}
        style={{ color: nivel === 'cidade' ? CORES.primaria : 'var(--color-text-muted)' }}
      >
        Cidade
      </span>
    </div>
  );
}

function TemporalActions({
  nivel,
  onNivelChange,
}: {
  nivel: PerformanceTempoNivel;
  onNivelChange: (nivel: PerformanceTempoNivel) => void;
}) {
  return (
    <div className="flex rounded-md border p-0.5" style={{ borderColor: 'var(--color-border)' }}>
      {NIVEIS_TEMPORAIS.map((item) => (
        <button
          key={item.valor}
          type="button"
          className="rounded px-2 py-1 text-xs font-semibold transition"
          style={{
            backgroundColor: nivel === item.valor ? 'var(--color-primary)' : 'transparent',
            color: nivel === item.valor ? '#fff' : 'var(--color-text-muted)',
          }}
          onClick={() => onNivelChange(item.valor)}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}

export default function PerformancePage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const [searchParams, setSearchParams] = useSearchParams();
  const filiais = useFiliais();

  const drillNivel = normalizeDrillNivel(searchParams.get('drillNivel'));
  const drillResponsavel = searchParams.get('drillResponsavel');
  const drillRegiao = searchParams.get('drillRegiao');
  const nivelTemporal = normalizeTemporalNivel(searchParams.get(NIVEL_PARAM));
  const anoTemporal = numeroParam(searchParams.get(ANO_PARAM));
  const mesTemporal = numeroParam(searchParams.get(MES_PARAM));

  const filtro: PerformanceFiltro = useMemo(() => ({
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    status: filtros.status,
    responsaveis: filtros.responsaveis,
    regioesDestino: filtros.regioesDestino,
    cidadesDestino: filtros.cidadesDestino,
  }), [
    dataFim,
    dataInicio,
    filtros.cidadesDestino,
    filtros.filiais,
    filtros.regioesDestino,
    filtros.responsaveis,
    filtros.status,
  ]);

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Status', count: filtros.status?.length ?? 0, onRemove: () => setFiltro('status', []) },
    { label: 'Responsáveis', count: filtros.responsaveis?.length ?? 0, onRemove: () => setFiltro('responsaveis', []) },
    { label: 'Regiões', count: filtros.regioesDestino?.length ?? 0, onRemove: () => setFiltro('regioesDestino', []) },
    { label: 'Cidades', count: filtros.cidadesDestino?.length ?? 0, onRemove: () => setFiltro('cidadesDestino', []) },
  ];

  const overview = usePerformanceOverview(filtro);
  const serieTemporal = usePerformanceSerieTemporal(filtro, nivelTemporal, anoTemporal, mesTemporal);
  const status = usePerformanceStatus(filtro);
  const historico = usePerformanceHistorico(filtro);
  const drilldown = usePerformanceDrilldown(filtro, {
    nivel: drillNivel,
    responsavel: drillResponsavel,
    regiaoDestino: drillRegiao,
  });
  const aging = usePerformanceAging(filtro);

  usePageHeader({
    title: 'Performance',
    description: 'Entregas por previsão, prazo, comprovantes e aging operacional.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const {
    kpis,
    serieTemporalData,
    statusData,
    historicoData,
    drilldownData,
    agingData,
  } = usePerformanceData({
    overview: overview.data,
    serieTemporal: serieTemporal.data,
    status: status.data,
    historico: historico.data,
    drilldown: drilldown.data,
    aging: aging.data,
  });

  const serieTemporalOption = useMemo(() => buildSerieTemporalOption(serieTemporalData, nivelTemporal), [nivelTemporal, serieTemporalData]);
  const statusOption = useMemo(() => buildStatusOption(statusData), [statusData]);
  const historicoOption = useMemo(() => buildHistoricoOption(historicoData), [historicoData]);
  const drilldownOption = useMemo(() => buildDrilldownOption(drilldownData, drillNivel), [drillNivel, drilldownData]);
  const agingOption = useMemo(() => buildAgingOption(agingData), [agingData]);

  const alterarNivelTemporal = useCallback((nivel: PerformanceTempoNivel) => {
    const next = new URLSearchParams(searchParams);
    next.set(NIVEL_PARAM, nivel);
    next.delete(ANO_PARAM);
    next.delete(MES_PARAM);
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);

  const drillTemporal = useCallback((data: string) => {
    const [ano, mes] = data.split('-');
    const next = new URLSearchParams(searchParams);
    if (nivelTemporal === 'ano') {
      next.set(NIVEL_PARAM, 'mes');
      next.set(ANO_PARAM, ano);
      next.delete(MES_PARAM);
    } else if (nivelTemporal === 'mes') {
      next.set(NIVEL_PARAM, 'dia');
      next.set(ANO_PARAM, ano);
      next.set(MES_PARAM, mes);
    }
    setSearchParams(next, { replace: true });
  }, [nivelTemporal, searchParams, setSearchParams]);

  function drillUp() {
    const next = new URLSearchParams(searchParams);
    if (drillNivel === 'cidade') {
      next.set('drillNivel', 'regiao');
      next.delete('drillRegiao');
    } else {
      next.delete('drillNivel');
      next.delete('drillResponsavel');
      next.delete('drillRegiao');
    }
    setSearchParams(next, { replace: true });
  }

  function drillDown(nome: string | null) {
    if (!nome || drillNivel === 'cidade') {
      return;
    }
    const next = new URLSearchParams(searchParams);
    if (drillNivel === 'responsavel') {
      next.set('drillNivel', 'regiao');
      next.set('drillResponsavel', nome);
      next.delete('drillRegiao');
    } else {
      next.set('drillNivel', 'cidade');
      next.set('drillRegiao', nome);
    }
    setSearchParams(next, { replace: true });
  }

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker
          label="Data de Previsão de Entrega"
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
          onRangeChange={setDataRange}
        />
        <AsyncMultiSelect
          label="Filiais"
          opcoes={filiais.data ?? []}
          selecionados={filtros.filiais ?? []}
          onChange={(valores) => setFiltro('filiais', valores)}
          isLoading={filiais.isLoading}
        />
        <AsyncMultiSelect
          label="Status"
          opcoes={STATUS_OPTIONS}
          selecionados={filtros.status ?? []}
          onChange={(valores) => setFiltro('status', valores)}
        />
        <TextListFilter
          label="Responsáveis"
          values={filtros.responsaveis}
          onChange={(valores) => setFiltro('responsaveis', valores)}
        />
        <TextListFilter
          label="Regiões"
          values={filtros.regioesDestino}
          onChange={(valores) => setFiltro('regioesDestino', valores)}
        />
        <TextListFilter
          label="Cidades"
          values={filtros.cidadesDestino}
          onChange={(valores) => setFiltro('cidadesDestino', valores)}
        />
      </FilterBar>

      {overview.isError && (
        <MensagemErro
          mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de performance.')}
          tipo={getTipoErro(overview.error)}
        />
      )}

      {overview.isLoading ? (
        <PerformanceKpiSkeleton />
      ) : (
        <div
          className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-12"
        >
          {kpis.map((kpi) => (
            <div key={kpi.label} className={`min-w-0 ${kpiGridSpan(kpi.label)}`}>
              <KpiCard
                label={kpi.label}
                valor={kpi.valor}
                valorClassName={kpiValorClassName(kpi.label)}
                helperText={kpi.helperText}
                tone={kpi.tone}
                helperTone={kpi.tone}
              />
            </div>
          ))}
        </div>
      )}

      <div className="mb-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div className="h-[25rem] min-h-0">
          <ChartWrapper
            titulo="Entregas por dia, mês e ano"
            option={serieTemporalOption}
            actions={<TemporalActions nivel={nivelTemporal} onNivelChange={alterarNivelTemporal} />}
            onEvents={{
              click: (params: unknown) => {
                const item = params as { name?: string };
                if (item.name) drillTemporal(item.name);
              },
            }}
            isLoading={serieTemporal.isLoading}
            isEmpty={serieTemporalData.length === 0}
            emptyMessage={PERFORMANCE_EMPTY_MESSAGE}
            erro={serieTemporal.isError ? getApiErrorMessage(serieTemporal.error, 'Erro ao carregar série temporal.') : null}
            altura="100%"
            className="h-full"
          />
        </div>
        <div className="h-[25rem] min-h-0">
          <ChartWrapper
            titulo="Distribuição por Status"
            option={statusOption}
            isLoading={status.isLoading}
            isEmpty={statusData.length === 0}
            emptyMessage={PERFORMANCE_EMPTY_MESSAGE}
            erro={status.isError ? getApiErrorMessage(status.error, 'Erro ao carregar status.') : null}
            altura="100%"
            className="h-full"
          />
        </div>
        <div className="h-[25rem] min-h-0">
          <ChartWrapper
            titulo="Histórico de Performance"
            option={historicoOption}
            isLoading={historico.isLoading}
            isEmpty={historicoData.length === 0}
            emptyMessage={PERFORMANCE_EMPTY_MESSAGE}
            erro={historico.isError ? getApiErrorMessage(historico.error, 'Erro ao carregar histórico.') : null}
            altura="100%"
            className="h-full"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div className="h-[26rem] min-h-0">
          <ChartWrapper
            titulo="Performance por responsável, região e cidade"
            actions={(
              <DrilldownActions
                nivel={drillNivel}
                responsavel={drillResponsavel}
                regiao={drillRegiao}
                onBack={drillUp}
              />
            )}
            option={drilldownOption}
            isLoading={drilldown.isLoading}
            isEmpty={drilldownData.length === 0}
            emptyMessage={PERFORMANCE_EMPTY_MESSAGE}
            erro={drilldown.isError ? getApiErrorMessage(drilldown.error, 'Erro ao carregar drill-down.') : null}
            altura="100%"
            className="h-full"
            onEvents={{
              click: (params) => drillDown(chartClickName(params)),
            }}
          />
        </div>
        <div className="h-[26rem] min-h-0">
          <ChartWrapper
            titulo="Entregas em aberto"
            option={agingOption}
            isLoading={aging.isLoading}
            isEmpty={agingData.length === 0}
            emptyMessage={PERFORMANCE_EMPTY_MESSAGE}
            erro={aging.isError ? getApiErrorMessage(aging.error, 'Erro ao carregar aging.') : null}
            altura="100%"
            className="h-full"
          />
        </div>
      </div>

      <span className="sr-only">
        Total de entregas carregadas: {formatarNumero(overview.data?.totalEntregas ?? 0)}
      </span>
    </div>
  );
}
