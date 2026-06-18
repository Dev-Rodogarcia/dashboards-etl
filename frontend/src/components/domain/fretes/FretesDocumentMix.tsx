import { useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import ChartWrapper from '../../charts/ChartWrapper';
import { useEchartsTheme } from '../../charts/useEchartsTheme';
import type { FretesDocumentMix as FretesDocumentMixType } from '../../../types/fretes';
import { buildBaseDonutOption, getEchartsThemeTokens } from '../../../utils/echartsBuilders';

interface FretesDocumentMixProps {
  dados: FretesDocumentMixType[];
  isLoading?: boolean;
}

export default function FretesDocumentMix({ dados, isLoading }: FretesDocumentMixProps) {
  const { isDark } = useEchartsTheme();

  const option: EChartsOption = useMemo(() => {
    const tokens = getEchartsThemeTokens(isDark);
    const coresDocumento: Record<string, string> = {
      'CT-e': tokens.palette[0],
      'NFS-e': tokens.palette[8],
      Pendente: tokens.mutedTextColor,
    };

    return buildBaseDonutOption(isDark, {
      tooltip: {
        trigger: 'item' as const,
        formatter: '{b}: {c} ({d}%)',
      },
      series: [
        {
          type: 'pie' as const,
          data: dados.map((d) => ({
            name: d.tipoDocumento,
            value: d.total,
            itemStyle: { color: coresDocumento[d.tipoDocumento] ?? tokens.palette[5] },
          })),
          label: {
            formatter: '{b}\n{d}%',
          },
        },
      ],
    });
  }, [dados, isDark]);

  return (
    <ChartWrapper
      titulo="Mix Documental"
      chartKey="fretesMixDocumental"
      option={option}
      isLoading={isLoading}
      isEmpty={dados.length === 0}
    />
  );
}
