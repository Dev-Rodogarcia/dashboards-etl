import { memo, useMemo } from 'react';
import type { CSSProperties, ReactNode } from 'react';
import type { EChartsOption } from 'echarts';
import ReactECharts from 'echarts-for-react';
import ChartCard from '../shared/ChartCard';
import type { ChartDictionaryKey } from '../../constants/chartDictionary';
import { useEchartsTheme } from './useEchartsTheme';

const ECHARTS_CANVAS_OPTS = { renderer: 'canvas' as const };

function mergePlainObject(base: unknown, override: unknown) {
  return { ...(base as object), ...(override as object) };
}

function mergeAxis(baseAxis: unknown, optionAxis: unknown) {
  const mergeSingleAxis = (axis: unknown) => {
    const base = baseAxis as Record<string, unknown>;
    const current = (axis ?? {}) as Record<string, unknown>;
    const baseAxisLine = base.axisLine as Record<string, unknown> | undefined;
    const currentAxisLine = current.axisLine as Record<string, unknown> | undefined;
    const baseSplitLine = base.splitLine as Record<string, unknown> | undefined;
    const currentSplitLine = current.splitLine as Record<string, unknown> | undefined;

    return {
      ...base,
      ...current,
      axisLabel: mergePlainObject(base.axisLabel, current.axisLabel),
      axisLine: {
        ...baseAxisLine,
        ...currentAxisLine,
        lineStyle: mergePlainObject(baseAxisLine?.lineStyle, currentAxisLine?.lineStyle),
      },
      splitLine: {
        ...baseSplitLine,
        ...currentSplitLine,
        lineStyle: mergePlainObject(baseSplitLine?.lineStyle, currentSplitLine?.lineStyle),
      },
      nameTextStyle: mergePlainObject(base.nameTextStyle, current.nameTextStyle),
    };
  };

  if (Array.isArray(optionAxis)) {
    return optionAxis.map(mergeSingleAxis);
  }
  return mergeSingleAxis(optionAxis);
}

interface ChartWrapperProps {
  titulo: string;
  option: EChartsOption;
  actions?: ReactNode;
  onEvents?: Record<string, (params: unknown) => void>;
  isLoading?: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  erro?: string | null;
  altura?: number | string;
  className?: string;
  contentClassName?: string;
  chartClassName?: string;
  chartKey?: ChartDictionaryKey;
  sideContent?: ReactNode;
}

function ChartWrapperInner({
  titulo,
  option,
  actions,
  onEvents,
  isLoading,
  isEmpty,
  emptyMessage,
  erro,
  altura = 300,
  className,
  contentClassName,
  chartClassName,
  chartKey,
  sideContent,
}: ChartWrapperProps) {
  const { baseOption } = useEchartsTheme();

  const mergedOption: EChartsOption = useMemo(() => ({
    ...baseOption,
    ...option,
    tooltip: { ...baseOption.tooltip, ...(option.tooltip as object) },
    legend: { ...baseOption.legend, ...(option.legend as object) },
    grid: { ...baseOption.grid, ...(option.grid as object) },
    xAxis: mergeAxis(baseOption.xAxis, option.xAxis),
    yAxis: mergeAxis(baseOption.yAxis, option.yAxis),
  }), [baseOption, option]);
  const chartStyle: CSSProperties = useMemo(() => ({
    height: altura,
    minHeight: typeof altura === 'number' ? altura : 300,
    width: '100%',
  }), [altura]);

  return (
    <ChartCard titulo={titulo} actions={actions} isLoading={isLoading} isEmpty={isEmpty} emptyMessage={emptyMessage} erro={erro} className={className} contentClassName={contentClassName} chartKey={chartKey}>
      <div className={sideContent ? 'grid h-full min-h-0 grid-cols-1 gap-4 lg:grid-cols-3 lg:gap-6' : 'h-full min-h-0'}>
        <div className={`h-full min-h-0 ${sideContent ? 'lg:col-span-2' : ''} ${chartClassName ?? ''}`} style={chartStyle}>
          <ReactECharts
            option={mergedOption}
            style={chartStyle}
            opts={ECHARTS_CANVAS_OPTS}
            onEvents={onEvents}
            notMerge
          />
        </div>
        {sideContent ? (
          <div className="min-h-0 lg:col-span-1">
            {sideContent}
          </div>
        ) : null}
      </div>
    </ChartCard>
  );
}

const ChartWrapper = memo(ChartWrapperInner);
export default ChartWrapper;
