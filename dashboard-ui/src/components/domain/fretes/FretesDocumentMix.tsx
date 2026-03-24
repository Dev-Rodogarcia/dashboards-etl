import ChartWrapper from '../../charts/ChartWrapper';
import type { FretesDocumentMix as FretesDocumentMixType } from '../../../types/fretes';
import { CORES } from '../../../utils/chartColors';

interface FretesDocumentMixProps {
  dados: FretesDocumentMixType[];
  isLoading?: boolean;
}

const CORES_DOC: Record<string, string> = {
  'CT-e': CORES.primaria,
  'NFS-e': CORES.alerta,
  Pendente: CORES.cinzaClaro,
};

export default function FretesDocumentMix({ dados, isLoading }: FretesDocumentMixProps) {
  const option = {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)',
    },
    series: [
      {
        type: 'pie' as const,
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: dados.map((d) => ({
          name: d.tipoDocumento,
          value: d.total,
          itemStyle: { color: CORES_DOC[d.tipoDocumento] ?? CORES.cinza },
        })),
        label: {
          formatter: '{b}\n{d}%',
          fontSize: 11,
        },
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' },
        },
      },
    ],
  };

  return (
    <ChartWrapper
      titulo="Mix Documental"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
