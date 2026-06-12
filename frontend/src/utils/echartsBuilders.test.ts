import { describe, expect, it } from 'vitest';
import { buildBaseDonutOption } from './echartsBuilders';

describe('buildBaseDonutOption', () => {
  it('preserva posicionamento e dimensoes especificos da serie', () => {
    const option = buildBaseDonutOption(false, {
      legend: { show: false },
      series: [{
        type: 'pie',
        radius: ['36%', '60%'],
        center: ['50%', '50%'],
        data: [{ name: 'Cliente A', value: 10 }],
      }],
    });
    const series = Array.isArray(option.series) ? option.series[0] : option.series;

    expect(series).toMatchObject({
      radius: ['36%', '60%'],
      center: ['50%', '50%'],
    });
    expect(option.legend).toMatchObject({ show: false });
  });
});
