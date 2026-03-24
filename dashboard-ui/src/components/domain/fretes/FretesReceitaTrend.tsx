import ChartWrapper from '../../charts/ChartWrapper';
import type { FretesTrendPoint } from '../../../types/fretes';
import { CORES } from '../../../utils/chartColors';
import { formatarDataCurta } from '../../../utils/formatadores';

interface FretesReceitaTrendProps {
  dados: FretesTrendPoint[];
  isLoading?: boolean;
}

export default function FretesReceitaTrend({ dados, isLoading }: FretesReceitaTrendProps) {
  const option = {
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
        itemStyle: { color: CORES.primaria },
        barMaxWidth: 30,
      },
      {
        name: 'Valor Frete',
        type: 'bar' as const,
        data: dados.map((d) => d.valorFrete),
        itemStyle: { color: CORES.alerta },
        barMaxWidth: 30,
      },
      {
        name: 'Fretes',
        type: 'line' as const,
        yAxisIndex: 1,
        data: dados.map((d) => d.fretes),
        itemStyle: { color: CORES.sucesso },
        smooth: true,
      },
    ],
  };

  return (
    <ChartWrapper
      titulo="Receita por Dia"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
