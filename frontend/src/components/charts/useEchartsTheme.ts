import { useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import { useTheme } from 'next-themes';
import { getEchartsThemeTokens } from '../../utils/echartsBuilders';

export function useEchartsTheme() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const baseOption: EChartsOption = useMemo(() => {
    const tokens = getEchartsThemeTokens(isDark);

    return {
      color: tokens.palette,
      textStyle: {
        fontFamily: 'Inter, system-ui, sans-serif',
        fontSize: 12,
        color: tokens.axisColor,
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
        backgroundColor: tokens.tooltipBg,
        borderColor: tokens.tooltipBorder,
        borderWidth: 1,
        textStyle: {
          color: tokens.tooltipText,
          fontSize: 12,
          textBorderWidth: 0,
          textShadowBlur: 0,
        },
      },
      legend: {
        bottom: 0,
        textStyle: {
          fontSize: 11,
          color: tokens.axisColor,
        },
      },
      xAxis: {
        axisLine: { lineStyle: { color: tokens.axisLineColor } },
        axisTick: { show: false },
        axisLabel: { color: tokens.axisColor, fontSize: 11, textBorderWidth: 0, textShadowBlur: 0 },
      },
      yAxis: {
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: tokens.splitLineColor } },
        axisLabel: { color: tokens.axisColor, fontSize: 11, textBorderWidth: 0, textShadowBlur: 0 },
        nameTextStyle: { color: tokens.axisColor, fontSize: 11, textBorderWidth: 0, textShadowBlur: 0 },
      },
    };
  }, [isDark]);

  return { baseOption, isDark };
}
