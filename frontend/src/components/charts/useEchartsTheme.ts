import { useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import { useTheme } from 'next-themes';
import { PALETA_SERIES } from '../../utils/chartColors';

export function useEchartsTheme() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const baseOption: EChartsOption = useMemo(() => {
    const axisColor = isDark ? '#94a3b8' : '#6b7280';
    const axisLineColor = isDark ? '#334155' : '#e5e7eb';
    const splitLineColor = isDark ? '#1f2937' : '#f3f4f6';
    const tooltipBg = isDark ? 'rgba(15, 23, 42, 0.96)' : 'rgba(255, 255, 255, 0.95)';
    const tooltipBorder = isDark ? '#334155' : '#e5e7eb';
    const tooltipText = isDark ? '#e5e7eb' : '#374151';
    const palette = isDark
      ? ['#60a5fa', '#f97316', '#34d399', '#f87171', '#a78bfa', '#22d3ee', '#f472b6', '#a3e635', '#facc15', '#c4b5fd']
      : [...PALETA_SERIES];

    return {
      color: palette,
      textStyle: {
        fontFamily: 'Inter, system-ui, sans-serif',
        fontSize: 12,
        color: axisColor,
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
        backgroundColor: tooltipBg,
        borderColor: tooltipBorder,
        borderWidth: 1,
        textStyle: {
          color: tooltipText,
          fontSize: 12,
        },
      },
      legend: {
        bottom: 0,
        textStyle: {
          fontSize: 11,
          color: axisColor,
        },
      },
      xAxis: {
        axisLine: { lineStyle: { color: axisLineColor } },
        axisTick: { show: false },
        axisLabel: { color: axisColor, fontSize: 11 },
      },
      yAxis: {
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: splitLineColor } },
        axisLabel: { color: axisColor, fontSize: 11 },
        nameTextStyle: { color: axisColor, fontSize: 11 },
      },
    };
  }, [isDark]);

  return { baseOption };
}
