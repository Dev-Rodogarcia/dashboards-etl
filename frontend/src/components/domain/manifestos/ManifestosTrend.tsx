import ChartWrapper from '../../charts/ChartWrapper';
import type { ManifestosTempoNivel, ManifestosTrendPoint } from '../../../types/manifestos';
import { CORES_STATUS } from '../../../utils/chartColors';

interface ManifestosTrendProps {
  dados: ManifestosTrendPoint[];
  nivel: ManifestosTempoNivel;
  onNivelChange: (nivel: ManifestosTempoNivel) => void;
  onPointClick: (data: string) => void;
  isLoading?: boolean;
}

const niveis: Array<{ valor: ManifestosTempoNivel; label: string }> = [
  { valor: 'ano', label: 'Ano' },
  { valor: 'mes', label: 'Mês' },
  { valor: 'dia', label: 'Dia' },
];

function formatarLabelTemporal(data: string, nivel: ManifestosTempoNivel): string {
  const [ano, mes, dia] = data.split('-');
  if (nivel === 'ano') return ano;
  if (nivel === 'mes') return `${mes}/${ano}`;
  return `${dia}/${mes}`;
}

export default function ManifestosTrend({ dados, nivel, onNivelChange, onPointClick, isLoading }: ManifestosTrendProps) {
  const option = {
    tooltip: {
      trigger: 'axis' as const,
    },
    legend: {
      top: 0,
    },
    grid: {
      top: 42,
      left: 10,
      right: 10,
      bottom: 10,
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      data: dados.map((d) => d.date),
      boundaryGap: false,
      axisLabel: {
        formatter: (value: string) => formatarLabelTemporal(value, nivel),
      },
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
      titulo="Status de Manifestos por dia, mês e ano"
      option={option}
      actions={
        <div className="flex rounded-md border p-0.5" style={{ borderColor: 'var(--color-border)' }}>
          {niveis.map((item) => (
            <button
              key={item.valor}
              type="button"
              className="rounded px-2 py-1 text-xs font-semibold transition"
              style={{
                backgroundColor: nivel === item.valor ? 'var(--color-primary)' : 'transparent',
                color: nivel === item.valor ? '#fff' : 'var(--color-text-muted)',
              }}
              onClick={() => onNivelChange(item.valor)}
            >
              {item.label}
            </button>
          ))}
        </div>
      }
      onEvents={{
        click: (params: unknown) => {
          const item = params as { name?: string };
          if (item.name) onPointClick(item.name);
        },
      }}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
