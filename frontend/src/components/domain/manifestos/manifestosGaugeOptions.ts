import type { EChartsOption } from 'echarts';
import { CORES } from '../../../utils/chartColors';
import { formatarPorcentagem } from '../../../utils/formatadores';

function percentualSeguro(valor: unknown): number {
  const numero = typeof valor === 'number' ? valor : Number(valor);
  if (!Number.isFinite(numero)) return 0;
  return Math.max(0, numero);
}

function limitarPreenchimento(valor: number): number {
  return Math.min(100, valor);
}

export function buildManifestosHalfDonutOption(globalInput: number, corDestaque: string = CORES.primaria): EChartsOption {
  const globalReal = percentualSeguro(globalInput);
  const globalPreenchimento = limitarPreenchimento(globalReal);
  const restante = Math.max(0, 100 - globalPreenchimento);
  const textoGlobal = formatarPorcentagem(globalReal, 1);

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const item = params as { name?: string; value?: number };
        if (!item.name || item.name === 'Metade Oculta') return '';
        const valor = item.name === 'Global' ? globalReal : Number(item.value ?? 0);
        return `${item.name}: ${formatarPorcentagem(valor, 1)}`;
      },
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '61%',
        silent: true,
        style: {
          text: textoGlobal,
          fill: 'var(--color-text)',
          fontSize: 29,
          fontWeight: 800,
        },
      },
      {
        type: 'text',
        left: '4%',
        top: '86%',
        silent: true,
        style: {
          text: formatarPorcentagem(0, 1),
          fill: 'var(--color-text-muted)',
          fontSize: 11,
          fontWeight: 600,
        },
      },
      {
        type: 'text',
        right: '3%',
        top: '86%',
        silent: true,
        style: {
          text: formatarPorcentagem(100, 1),
          fill: 'var(--color-text-muted)',
          fontSize: 11,
          fontWeight: 600,
        },
      },
    ],
    series: [
      {
        type: 'pie',
        radius: ['80%', '112%'],
        center: ['50%', '87%'],
        startAngle: 180,
        avoidLabelOverlap: false,
        label: {
          show: false,
        },
        labelLine: { show: false },
        emphasis: { disabled: true },
        data: [
          {
            name: 'Global',
            value: globalPreenchimento,
            itemStyle: { color: corDestaque, borderWidth: 0 },
          },
          {
            name: 'Restante',
            value: restante,
            itemStyle: { color: 'rgba(148, 163, 184, 0.22)', borderWidth: 0 },
          },
          {
            name: 'Metade Oculta',
            value: 100,
            label: { show: false },
            labelLine: { show: false },
            itemStyle: { color: 'transparent', borderWidth: 0 },
            emphasis: { disabled: true },
            select: { disabled: true },
          },
        ],
      },
    ],
  };
}
