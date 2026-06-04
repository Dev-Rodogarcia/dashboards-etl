import { useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import ChartWrapper from '../../charts/ChartWrapper';
import type { ColetasTrendPoint } from '../../../types/coletas';
import { CORES } from '../../../utils/chartColors';
import { formatarDataCurta } from '../../../utils/formatadores';

interface ColetasTrendProps {
  dados: ColetasTrendPoint[];
  isLoading?: boolean;
}

export default function ColetasTrend({ dados, isLoading }: ColetasTrendProps) {
  const option: EChartsOption = useMemo(() => ({
    xAxis: {
      type: 'category' as const,
      data: dados.map((d) => formatarDataCurta(d.date)),
      boundaryGap: false,
    },
    yAxis: {
      type: 'value' as const,
      name: 'Qtd',
    },
    series: [
      {
        name: 'Total',
        type: 'line' as const,
        data: dados.map((d) => d.total),
        itemStyle: { color: CORES.primaria },
        lineStyle: { width: 2 },
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 5,
        smooth: true,
      },
      {
        name: 'Finalizadas',
        type: 'line' as const,
        data: dados.map((d) => d.finalizadas),
        itemStyle: { color: CORES.sucesso },
        lineStyle: { width: 2 },
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 5,
        smooth: true,
      },
      {
        name: 'Canceladas',
        type: 'line' as const,
        data: dados.map((d) => d.canceladas),
        itemStyle: { color: CORES.perigo },
        lineStyle: { width: 2 },
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 5,
        smooth: true,
      },
      {
        name: 'Em Tratativa',
        type: 'line' as const,
        data: dados.map((d) => d.emTratativa),
        itemStyle: { color: CORES.alerta },
        lineStyle: { width: 2 },
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 5,
        smooth: true,
      },
    ],
  }), [dados]);

  return (
    <ChartWrapper
      titulo="Coletas por dia, mês e ano"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
