import { useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import ChartWrapper from '../../charts/ChartWrapper';
import { useEchartsTheme } from '../../charts/useEchartsTheme';
import type { FretesTrendPoint } from '../../../types/fretes';
import { buildBaseBarOption, buildBaseLineOption, getEchartsThemeTokens } from '../../../utils/echartsBuilders';
import { formatarDataCurta } from '../../../utils/formatadores';

interface FretesReceitaTrendProps {
  dados: FretesTrendPoint[];
  isLoading?: boolean;
}

export default function FretesReceitaTrend({ dados, isLoading }: FretesReceitaTrendProps) {
  const { isDark } = useEchartsTheme();

  const option: EChartsOption = useMemo(() => {
    const tokens = getEchartsThemeTokens(isDark);

    return buildBaseLineOption(isDark, buildBaseBarOption(isDark, {
      xAxis: {
        type: 'category' as const,
        data: dados.map((d) => formatarDataCurta(d.date)),
      },
      yAxis: [
        { type: 'value' as const, name: 'R$' },
        { type: 'value' as const, name: 'Qtd', alignTicks: true },
      ],
      series: [
        {
          name: 'Receita Bruta',
          type: 'bar' as const,
          data: dados.map((d) => d.receitaBruta),
          itemStyle: { color: tokens.palette[0] },
        },
        {
          name: 'Valor Frete',
          type: 'bar' as const,
          data: dados.map((d) => d.valorFrete),
          itemStyle: { color: tokens.palette[8] },
        },
        {
          name: 'Fretes',
          type: 'line' as const,
          yAxisIndex: 1,
          data: dados.map((d) => d.fretes),
          itemStyle: { color: tokens.palette[2] },
          smooth: true,
        },
      ],
    }));
  }, [dados, isDark]);

  return (
    <ChartWrapper
      titulo="Receita por Dia"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
