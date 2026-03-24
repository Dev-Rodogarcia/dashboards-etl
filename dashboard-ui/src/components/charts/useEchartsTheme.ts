import type { EChartsOption } from 'echarts';
import { PALETA_SERIES } from '../../utils/chartColors';

export function useEchartsTheme() {
  const baseOption: EChartsOption = {
    color: [...PALETA_SERIES],
    textStyle: {
      fontFamily: 'Inter, system-ui, sans-serif',
      fontSize: 12,
    },
    grid: {
      top: 40,
      right: 20,
      bottom: 30,
      left: 50,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: '#374151',
        fontSize: 12,
      },
    },
    legend: {
      bottom: 0,
      textStyle: {
        fontSize: 11,
        color: '#6b7280',
      },
    },
    xAxis: {
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11 },
    },
    yAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#6b7280', fontSize: 11 },
    },
  };

  return { baseOption };
}
