import type { EChartsOption } from 'echarts';
import { getGoalToneStyle, resolverTomMetaPorProgresso, calcularProgressoMeta, type GoalMode } from './indicadoresGestaoVistaUi';

interface RankingOptionArgs<T> {
  items: T[];
  getLabel: (item: T) => string;
  getValue: (item: T) => number;
  threshold: number;
  mode: GoalMode;
  thresholdLabel: string;
  tooltipLines?: (item: T) => string[];
  valueFormatter?: (value: number) => string;
  axisFormatter?: (value: number) => string;
  max?: number;
}

interface MetaComparisonOptionArgs {
  label: string;
  value: number;
  threshold: number;
  mode: GoalMode;
  thresholdLabel: string;
  valueFormatter?: (value: number) => string;
  axisFormatter?: (value: number) => string;
  max?: number;
}

function truncarRotulo(label: string, limite = 26): string {
  if (label.length <= limite) {
    return label;
  }
  return `${label.slice(0, limite - 1)}…`;
}

function resolveColor(value: number, threshold: number, mode: GoalMode): string {
  const tone = resolverTomMetaPorProgresso(calcularProgressoMeta(value, threshold, mode));
  return getGoalToneStyle(tone).fill;
}

function defaultAxisFormatter(value: number): string {
  return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;
}

function defaultValueFormatter(value: number): string {
  return `${value.toLocaleString('pt-BR', {
    minimumFractionDigits: value % 1 === 0 ? 0 : 1,
    maximumFractionDigits: 1,
  })}%`;
}

function resolveMax(values: number[], threshold: number, max?: number): number {
  if (typeof max === 'number') {
    return max;
  }
  const base = Math.max(threshold, ...values, 0);
  if (base <= 1) {
    return 1;
  }
  return Math.min(Math.ceil(base * 1.2), 200);
}

function resolveChartValue(value: unknown): number {
  if (typeof value === 'number') {
    return value;
  }
  if (Array.isArray(value) && typeof value[0] === 'number') {
    return value[0];
  }
  return Number(value ?? 0);
}

export function buildRankingOption<T>({
  items,
  getLabel,
  getValue,
  threshold,
  mode,
  thresholdLabel,
  tooltipLines,
  valueFormatter = defaultValueFormatter,
  axisFormatter = defaultAxisFormatter,
  max,
}: RankingOptionArgs<T>): EChartsOption {
  const topItems = items.slice(0, 8);
  const ordered = [...topItems].reverse();
  const labels = ordered.map((item) => truncarRotulo(getLabel(item)));
  const values = ordered.map((item) => getValue(item));
  const resolvedMax = resolveMax(values, threshold, max);

  return {
    grid: { left: 110, right: 32, top: 20, bottom: 32 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const [first] = Array.isArray(params) ? params : [];
        const index = typeof first === 'object' && first !== null && 'dataIndex' in first
          ? Number((first as { dataIndex: number }).dataIndex)
          : -1;
        const item = ordered[index];
        if (!item) {
          return '';
        }
        const lines = [`<strong>${getLabel(item)}</strong>`, valueFormatter(getValue(item))];
        if (tooltipLines) {
          lines.push(...tooltipLines(item));
        }
        return lines.join('<br/>');
      },
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: resolvedMax,
      axisLabel: { formatter: (value: number) => axisFormatter(Number(value)) },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.16)' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        data: ordered.map((item) => ({
          value: getValue(item),
          itemStyle: { color: resolveColor(getValue(item), threshold, mode) },
        })),
        barMaxWidth: 24,
        label: {
          show: true,
          position: 'right',
          color: 'var(--color-text)',
          formatter: (params: { value?: unknown }) => valueFormatter(resolveChartValue(params.value)),
        },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: '#64748b', type: 'dashed', width: 2 },
          label: {
            formatter: thresholdLabel,
            color: '#64748b',
            backgroundColor: '#ffffff',
            padding: [2, 6],
            borderRadius: 999,
          },
          data: [{ xAxis: threshold }],
        },
      },
    ],
  };
}

export function buildMetaComparisonOption({
  label,
  value,
  threshold,
  mode,
  thresholdLabel,
  valueFormatter = defaultValueFormatter,
  axisFormatter = defaultAxisFormatter,
  max,
}: MetaComparisonOptionArgs): EChartsOption {
  const resolvedMax = resolveMax([value], threshold, max);

  return {
    grid: { left: 110, right: 32, top: 20, bottom: 20 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const items = Array.isArray(params) ? params : [];
        return items
          .map((item) => {
            const point = item as { name: string; value: number };
            return `<strong>${point.name}</strong><br/>${valueFormatter(Number(point.value))}`;
          })
          .join('<br/><br/>');
      },
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: resolvedMax,
      axisLabel: { formatter: (axisValue: number) => axisFormatter(Number(axisValue)) },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.16)' } },
    },
    yAxis: {
      type: 'category',
      data: [thresholdLabel, label],
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        data: [
          {
            value: threshold,
            itemStyle: { color: '#94a3b8' },
          },
          {
            value,
            itemStyle: { color: resolveColor(value, threshold, mode) },
          },
        ],
        barMaxWidth: 28,
        label: {
          show: true,
          position: 'right',
          color: 'var(--color-text)',
          formatter: (params: { value?: unknown }) => valueFormatter(resolveChartValue(params.value)),
        },
      },
    ],
  };
}
