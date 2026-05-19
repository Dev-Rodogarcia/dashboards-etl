import { memo } from 'react';
import type { ReactNode } from 'react';
import type { EChartsOption } from 'echarts';
import ReactECharts from 'echarts-for-react';
import ChartCard from '../shared/ChartCard';
import { useEchartsTheme } from './useEchartsTheme';

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
}: ChartWrapperProps) {
  const { baseOption } = useEchartsTheme();

  function mergePlainObject(base: unknown, override: unknown) {
    return { ...(base as object), ...(override as object) };
  }

  const mergeAxis = (baseAxis: unknown, optionAxis: unknown) => {
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
  };

  const mergedOption: EChartsOption = {
    ...baseOption,
    ...option,
    tooltip: { ...baseOption.tooltip, ...(option.tooltip as object) },
    legend: { ...baseOption.legend, ...(option.legend as object) },
    grid: { ...baseOption.grid, ...(option.grid as object) },
    xAxis: mergeAxis(baseOption.xAxis, option.xAxis),
    yAxis: mergeAxis(baseOption.yAxis, option.yAxis),
  };
  const chartHeight = typeof altura === 'number' ? altura : altura;

  return (
    <ChartCard titulo={titulo} actions={actions} isLoading={isLoading} isEmpty={isEmpty} emptyMessage={emptyMessage} erro={erro} className={className}>
      <div className="h-full min-h-0">
        <ReactECharts
          option={mergedOption}
          style={{ height: chartHeight }}
          opts={{ renderer: 'canvas' }}
          onEvents={onEvents}
          notMerge
        />
      </div>
    </ChartCard>
  );
}

const ChartWrapper = memo(ChartWrapperInner);
export default ChartWrapper;
