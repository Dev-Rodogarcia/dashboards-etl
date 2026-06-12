import type { EChartsOption } from 'echarts';
import type { ManifestosCustosEvolucao } from '../../../types/manifestos';
import { buildBaseLineOption, getEchartsThemeTokens } from '../../../utils/echartsBuilders';
import { formatarMoeda } from '../../../utils/formatadores';
import type { GoalTone } from '../../../utils/indicadoresGestaoVistaUi';

function formatarDataCurta(data: string): string {
  const [ano, mes, dia] = data.split('-');
  return dia && mes && ano ? `${dia}/${mes}` : data;
}

export function metaCustoDisponivel(dados: ManifestosCustosEvolucao): boolean {
  return dados.orcamentoAplicavel && dados.orcamentoConfigurado;
}

export function resolverTomCusto(
  valor: number,
  meta: number,
  comparacaoDisponivel: boolean,
): GoalTone {
  if (!comparacaoDisponivel || !Number.isFinite(valor) || !Number.isFinite(meta)) {
    return 'neutral';
  }
  return valor <= meta ? 'positive' : 'negative';
}

export function buildManifestosCustoEvolutionOption(
  dados: ManifestosCustosEvolucao,
  isDark: boolean,
): EChartsOption {
  const tokens = getEchartsThemeTokens(isDark);
  const serie = [...dados.serieDiaria]
    .sort((left, right) => left.data.localeCompare(right.data));
  const temMeta = metaCustoDisponivel(dados);
  const ultimoCusto = serie.at(-1)?.custoReal ?? dados.custoMedioDiarioReal;
  const custoDentroDaMeta = temMeta && ultimoCusto <= dados.limiteDiarioDinamico;
  const corCustoReal = temMeta
    ? tokens.palette[custoDentroDaMeta ? 2 : 3]
    : tokens.palette[0];
  const series: NonNullable<EChartsOption['series']> = [
    {
      name: 'Custo Real',
      type: 'line',
      data: serie.map((item) => item.custoReal),
      symbol: 'none',
      lineStyle: {
        color: corCustoReal,
        type: 'solid',
      },
      itemStyle: {
        color: corCustoReal,
      },
    },
  ];

  if (temMeta) {
    series.push({
      name: 'Meta Diária Dinâmica',
      type: 'line',
      data: serie.map(() => dados.limiteDiarioDinamico),
      symbol: 'none',
      lineStyle: {
        color: tokens.palette[0],
        type: 'dashed',
      },
      itemStyle: {
        color: tokens.palette[0],
      },
    });
  }

  return buildBaseLineOption(isDark, {
    legend: {
      top: 0,
    },
    grid: {
      top: 48,
      right: 28,
      bottom: 22,
      left: 18,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: serie.map((item) => item.data),
      axisLabel: {
        formatter: (value: string) => formatarDataCurta(value),
      },
    },
    yAxis: {
      type: 'value',
      name: 'R$',
    },
    series,
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const items = Array.isArray(params)
          ? params as Array<{
              axisValue?: string;
              marker?: string;
              seriesName?: string;
              value?: number | { value?: number };
            }>
          : [];
        const data = items[0]?.axisValue ? `<strong>${formatarDataCurta(items[0].axisValue)}</strong>` : '';
        const valores = items.map((item) => {
          const valor = typeof item.value === 'object' ? item.value?.value : item.value;
          return `${item.marker ?? ''}${item.seriesName ?? ''}: ${formatarMoeda(Number(valor ?? 0))}`;
        });
        return [data, ...valores].filter(Boolean).join('<br/>');
      },
    },
  });
}
