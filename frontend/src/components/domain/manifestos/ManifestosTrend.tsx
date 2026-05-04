import ChartWrapper from '../../charts/ChartWrapper';
import type { ManifestosTrendPoint } from '../../../types/manifestos';
import { CORES_STATUS } from '../../../utils/chartColors';
import { formatarDataCurta } from '../../../utils/formatadores';

interface ManifestosTrendProps {
  dados: ManifestosTrendPoint[];
  isLoading?: boolean;
}

export default function ManifestosTrend({ dados, isLoading }: ManifestosTrendProps) {
  const option = {
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
        name: 'Encerrado',
        type: 'line' as const,
        stack: 'total',
        areaStyle: {},
        data: dados.map((d) => d.encerrado),
        itemStyle: { color: CORES_STATUS['encerrado'] },
        smooth: true,
      },
      {
        name: 'Em Trânsito',
        type: 'line' as const,
        stack: 'total',
        areaStyle: {},
        data: dados.map((d) => d.emTransito),
        itemStyle: { color: CORES_STATUS['em trânsito'] },
        smooth: true,
      },
      {
        name: 'Pendente',
        type: 'line' as const,
        stack: 'total',
        areaStyle: {},
        data: dados.map((d) => d.pendente),
        itemStyle: { color: CORES_STATUS['pendente'] },
        smooth: true,
      },
    ],
  };

  return (
    <ChartWrapper
      titulo="Manifestos por Dia"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
