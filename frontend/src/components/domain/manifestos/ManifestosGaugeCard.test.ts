import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import type { GaugeMetric } from '../../../types/manifestos';
import ManifestosGaugeCard from './ManifestosGaugeCard';
import { buildManifestosHalfDonutOption } from './manifestosGaugeOptions';

describe('ManifestosGaugeCard', () => {
  it('monta half-donut do ECharts com fatia transparente inferior', () => {
    const option = buildManifestosHalfDonutOption(72.5, '#059669');
    const series = Array.isArray(option.series) ? option.series[0] : undefined as unknown;

    expect(series).toMatchObject({
      type: 'pie',
      startAngle: 180,
    });
    expect(series).not.toHaveProperty('endAngle');

    const data = (series as { data?: Array<{ name: string; value: number; itemStyle?: { color?: string } }> }).data ?? [];
    expect(data).toHaveLength(3);
    expect(data.reduce((total, item) => total + item.value, 0)).toBe(200);
    expect(data[0]).toMatchObject({ name: 'Global', value: 72.5, itemStyle: { color: '#059669' } });
    expect(data[1]).toMatchObject({ name: 'Restante', value: 27.5 });
    expect(data[2]).toMatchObject({ name: 'Metade Oculta', value: 100, itemStyle: { color: 'transparent' } });

    const graphic = (option.graphic as Array<{ style?: { text?: string } }> | undefined) ?? [];
    const textos = graphic.map((item) => item.style?.text);
    expect(textos).toEqual(expect.arrayContaining(['72,5%', '0,0%', '100,0%']));
  });

  it('limita o percentual global entre zero e cem', () => {
    const option = buildManifestosHalfDonutOption(140);
    const series = Array.isArray(option.series) ? option.series[0] : undefined as unknown;
    const data = (series as { data?: Array<{ value: number }> }).data ?? [];

    expect(data[0].value).toBe(100);
    expect(data[1].value).toBe(0);
  });

  it('renderiza no DOM o global e os tres desdobramentos vindos do GaugeMetric', () => {
    const metric: GaugeMetric = {
      global: 74.6,
      distribuicao: 80.1,
      transferencia: 65.7,
      cargaFechada: 70.5,
    };

    const html = renderToStaticMarkup(
      createElement(ManifestosGaugeCard, {
        titulo: 'Aproveitamento',
        metric,
      }),
    );

    expect(html).toContain('Global: 74,6%');
    expect(html).toContain('80,1%');
    expect(html).toContain('Distribuição');
    expect(html).toContain('65,7%');
    expect(html).toContain('Transferência');
    expect(html).toContain('70,5%');
    expect(html).toContain('Carga Fechada');
  });

  it('renderiza 0,0% quando metricas chegam nulas, ausentes ou invalidas', () => {
    const metric = {
      global: Number.NaN,
      distribuicao: undefined,
      transferencia: null,
      cargaFechada: 'invalido',
    } as unknown as GaugeMetric;

    const html = renderToStaticMarkup(
      createElement(ManifestosGaugeCard, {
        titulo: 'Remuneração',
        metric,
      }),
    );

    expect((html.match(/0,0%/g) ?? []).length).toBeGreaterThanOrEqual(4);
  });
});
