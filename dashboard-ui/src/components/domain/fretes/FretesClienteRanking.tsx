import type { EChartsOption } from 'echarts';
import ChartWrapper from '../../charts/ChartWrapper';
import type { FretesClienteRanking as FretesClienteRankingType } from '../../../types/fretes';
import { CORES } from '../../../utils/chartColors';

interface FretesClienteRankingProps {
  dados: FretesClienteRankingType[];
  isLoading?: boolean;
}

export default function FretesClienteRanking({ dados, isLoading }: FretesClienteRankingProps) {
  const dadosRevertidos = [...dados].reverse();

  const option: EChartsOption = {
    grid: { left: 10, containLabel: true },
    xAxis: { type: 'value', name: 'R$' },
    yAxis: {
      type: 'category',
      data: dadosRevertidos.map((d) =>
        d.cliente.length > 25 ? `${d.cliente.slice(0, 25)}...` : d.cliente
      ),
    },
    series: [
      {
        name: 'Receita',
        type: 'bar',
        data: dadosRevertidos.map((d) => d.receita),
        itemStyle: { color: CORES.primaria },
        barMaxWidth: 20,
      },
    ],
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const itens = Array.isArray(params) ? params as Array<{ name: string; value: number }> : [];
        const item = itens[0];
        if (!item) {
          return '';
        }

        const cliente = dadosRevertidos.find((d) =>
          d.cliente.startsWith(item.name.replace('...', ''))
        );

        if (!cliente) {
          return '';
        }

        return `${cliente.cliente}<br/>Receita: R$ ${cliente.receita.toLocaleString('pt-BR')}<br/>Fretes: ${cliente.fretes}<br/>Ticket: R$ ${cliente.ticketMedio.toLocaleString('pt-BR')}`;
      },
    },
  };

  return (
    <ChartWrapper
      titulo="Top Clientes por Receita"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
      altura={350}
    />
  );
}
