import { describe, expect, it } from 'vitest';
import type { EChartsOption } from 'echarts';
import type { ManifestosCustosEvolucao } from '../../../types/manifestos';
import { getEchartsThemeTokens } from '../../../utils/echartsBuilders';
import {
  buildManifestosCustoEvolutionOption,
  resolverTomCusto,
} from './manifestosCustoUi';

function criarDados(
  ultimoCusto: number,
  limiteDiarioDinamico = 25,
  limiteDiarioBase = 100,
): ManifestosCustosEvolucao {
  return {
    orcamentoAplicavel: true,
    orcamentoConfigurado: true,
    observacao: null,
    totalDiasUteis: 20,
    diasUteisDecorridos: 10,
    diasUteisRestantes: 10,
    orcamentoCusto: 2000,
    custoReal: 900,
    limiteDiarioBase,
    custoMedioDiarioReal: 90,
    saldoOrcamentario: 1100,
    limiteDiarioDinamico,
    tendenciaCusto: 1800,
    consumoOrcamento: 45,
    serieDiaria: [
      { data: '2026-05-01', custoReal: 80 },
      { data: '2026-05-02', custoReal: ultimoCusto },
    ],
  };
}

function obterSeries(option: EChartsOption) {
  return Array.isArray(option.series) ? option.series : [option.series];
}

function obterCorCustoReal(series: ReturnType<typeof obterSeries>[number]) {
  const seriesComItemStyle = series as { itemStyle?: { color?: unknown } } | undefined;
  if (typeof seriesComItemStyle?.itemStyle?.color !== 'function') {
    throw new Error('A serie de Custo Real deve configurar itemStyle.color como callback.');
  }
  return seriesComItemStyle.itemStyle.color as (params: { value: number; dataIndex: number }) => string;
}

describe('ManifestosCustoEvolutionCard', () => {
  it('renderiza custo real em barras verdes quando o custo e menor ou igual a meta diaria base', () => {
    const option = buildManifestosCustoEvolutionOption(criarDados(100), false);
    const [custoReal, meta] = obterSeries(option);
    const tokens = getEchartsThemeTokens(false);

    expect(custoReal).toMatchObject({
      name: 'Custo Real',
      type: 'bar',
      data: [80, 100],
      barMaxWidth: 30,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
      },
    });
    expect(obterCorCustoReal(custoReal)({ value: 100, dataIndex: 1 })).toBe(tokens.palette[2]);
    expect(meta).toMatchObject({
      name: 'Meta Diária Base',
      type: 'line',
      data: [100, 100],
      lineStyle: {
        type: 'dashed',
      },
    });
  });

  it('renderiza barra vermelha quando o custo ultrapassa a meta diaria base', () => {
    const option = buildManifestosCustoEvolutionOption(criarDados(100.01, 1000), false);
    const [custoReal] = obterSeries(option);

    expect(obterCorCustoReal(custoReal)({ value: 100.01, dataIndex: 1 })).toBe(getEchartsThemeTokens(false).palette[3]);
  });

  it('nao desenha uma meta artificial quando o orcamento esta indisponivel', () => {
    const dados = criarDados(120);
    dados.orcamentoAplicavel = false;
    dados.orcamentoConfigurado = false;

    const option = buildManifestosCustoEvolutionOption(dados, false);
    const series = obterSeries(option);

    expect(series).toHaveLength(1);
    expect(series[0]).toMatchObject({
      name: 'Custo Real',
      type: 'bar',
    });
    expect(obterCorCustoReal(series[0])({ value: 120, dataIndex: 1 })).toBe(getEchartsThemeTokens(false).palette[0]);
  });

  it('mantem a inversao de cores dos cards de custo', () => {
    expect(resolverTomCusto(100, 100, true)).toBe('positive');
    expect(resolverTomCusto(99.99, 100, true)).toBe('positive');
    expect(resolverTomCusto(100.01, 100, true)).toBe('negative');
    expect(resolverTomCusto(100.01, 100, false)).toBe('neutral');
  });
});
