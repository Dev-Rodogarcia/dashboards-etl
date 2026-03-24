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

  const mergedOption: EChartsOption = {
    ...baseOption,
    ...option,
    tooltip: { ...baseOption.tooltip, ...(option.tooltip as object) },
    grid: { ...baseOption.grid, ...(option.grid as object) },
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
