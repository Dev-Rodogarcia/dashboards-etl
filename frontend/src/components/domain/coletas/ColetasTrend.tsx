import { useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import ChartWrapper from '../../charts/ChartWrapper';
import { useEchartsTheme } from '../../charts/useEchartsTheme';
import type { ColetasTrendPoint } from '../../../types/coletas';
import { buildBaseLineOption, getEchartsThemeTokens } from '../../../utils/echartsBuilders';
import { formatarDataCurta, formatarNumero } from '../../../utils/formatadores';

interface ColetasTrendProps {
  dados: ColetasTrendPoint[];
  isLoading?: boolean;
}

export default function ColetasTrend({ dados, isLoading }: ColetasTrendProps) {
  const { isDark } = useEchartsTheme();

  const option: EChartsOption = useMemo(() => {
    const tokens = getEchartsThemeTokens(isDark);
    const totalColetas = dados.reduce((acc, item) => acc + item.total, 0);
    const mediaTotal = dados.length > 0 ? totalColetas / dados.length : 0;
    const mediaTotalFormatada = formatarNumero(mediaTotal, Number.isInteger(mediaTotal) ? 0 : 1);

    return buildBaseLineOption(isDark, {
      legend: {
        formatter: (name: string) => (name === 'Média do período' ? `${name}: ${mediaTotalFormatada}` : name),
      },
      tooltip: {
        trigger: 'axis',
      },
      grid: {
        left: 26,
        right: 26,
      },
      xAxis: {
        type: 'category' as const,
        data: dados.map((d) => formatarDataCurta(d.date)),
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
          itemStyle: { color: tokens.palette[0] },
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 5,
          smooth: true,
          markLine: {
            silent: true,
            symbol: 'none',
            data: [{ type: 'average', name: 'Média do período' }],
            lineStyle: { width: 2, type: 'dashed', color: tokens.mutedTextColor, opacity: 0.38 },
            label: { show: false },
          },
        },
        {
          name: 'Finalizadas',
          type: 'line' as const,
          data: dados.map((d) => d.finalizadas),
          itemStyle: { color: tokens.palette[2] },
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 5,
          smooth: true,
        },
        {
          name: 'Canceladas',
          type: 'line' as const,
          data: dados.map((d) => d.canceladas),
          itemStyle: { color: tokens.palette[3] },
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 5,
          smooth: true,
        },
        {
          name: 'Em Tratativa',
          type: 'line' as const,
          data: dados.map((d) => d.emTratativa),
          itemStyle: { color: tokens.palette[8] },
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 5,
          smooth: true,
        },
        {
          name: 'Média do período',
          type: 'line' as const,
          data: [],
          itemStyle: { color: tokens.mutedTextColor, opacity: 0.38 },
          lineStyle: { width: 2, type: 'dashed', color: tokens.mutedTextColor, opacity: 0.38 },
          symbol: 'none',
          smooth: false,
          silent: true,
          z: 1,
        },
      ],
    });
  }, [dados, isDark]);

  return (
    <ChartWrapper
      titulo="Coletas por dia, mês e ano"
      chartKey="coletasSerie"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
