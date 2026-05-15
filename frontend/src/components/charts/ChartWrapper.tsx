import { memo } from 'react';
import type { EChartsOption } from 'echarts';
import ReactECharts from 'echarts-for-react';
import ChartCard from '../shared/ChartCard';
import { useEchartsTheme } from './useEchartsTheme';

interface ChartWrapperProps {
  titulo: string;
  option: EChartsOption;
  isLoading?: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  erro?: string | null;
  altura?: number;
  className?: string;
}

function ChartWrapperInner({
  titulo,
  option,
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

  return (
    <ChartCard titulo={titulo} isLoading={isLoading} isEmpty={isEmpty} emptyMessage={emptyMessage} erro={erro} className={className}>
      <ReactECharts
        option={mergedOption}
        style={{ height: altura }}
        opts={{ renderer: 'canvas' }}
        notMerge
      />
    </ChartCard>
  );
}

const ChartWrapper = memo(ChartWrapperInner);
export default ChartWrapper;
