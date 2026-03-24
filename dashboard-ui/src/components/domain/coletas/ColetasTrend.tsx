import ChartWrapper from '../../charts/ChartWrapper';
import type { ColetasTrendPoint } from '../../../types/coletas';
import { CORES } from '../../../utils/chartColors';
import { formatarDataCurta } from '../../../utils/formatadores';

interface ColetasTrendProps {
  dados: ColetasTrendPoint[];
  isLoading?: boolean;
}

export default function ColetasTrend({ dados, isLoading }: ColetasTrendProps) {
  const option = {
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
        itemStyle: { color: CORES.primaria },
        smooth: true,
      },
      {
        name: 'Finalizadas',
        type: 'line' as const,
        data: dados.map((d) => d.finalizadas),
        itemStyle: { color: CORES.sucesso },
        smooth: true,
      },
      {
        name: 'Canceladas',
        type: 'line' as const,
        data: dados.map((d) => d.canceladas),
        itemStyle: { color: CORES.perigo },
        smooth: true,
      },
      {
        name: 'Em Tratativa',
        type: 'line' as const,
        data: dados.map((d) => d.emTratativa),
        itemStyle: { color: CORES.alerta },
        smooth: true,
      },
    ],
  };

  return (
    <ChartWrapper
      titulo="Coletas por Dia"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
