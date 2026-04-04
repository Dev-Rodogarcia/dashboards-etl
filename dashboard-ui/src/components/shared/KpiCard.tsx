import { createElement, type ReactNode } from 'react';
import { resolveKpiIcon } from './kpiIconResolver';
import { getGoalToneStyle, type GoalTone } from '../../utils/indicadoresGestaoVistaUi';

function getFlexBasis(valor: string): number {
  if (valor.length > 14) return 190;
  if (valor.length > 8) return 155;
  return 110;
}

interface KpiCardProps {
  label: string;
  valor: string;
  icone?: ReactNode;
  tone?: GoalTone;
  progressPct?: number | null;
  trend?: {
    valor: number;
    direcao: 'up' | 'down' | 'neutral';
  };
}

export default function KpiCard({ label, valor, icone, tone = 'neutral', progressPct, trend }: KpiCardProps) {
  const flexBasis = getFlexBasis(valor);
  const style = getGoalToneStyle(tone);
  const secondaryColor = tone === 'neutral' ? 'var(--color-text-subtle)' : style.text;
  const iconNode = icone ?? createElement(resolveKpiIcon(label), { size: 16, 'aria-hidden': 'true' });
  const widthPct = Math.max(0, Math.min(progressPct ?? 0, 100));

  return (
    <div
      className="flex flex-col gap-1 rounded-[20px] border p-3 transition-all duration-150 hover:shadow-lg hover:-translate-y-[2px] cursor-default"
      style={{
        backgroundColor: tone === 'neutral' ? 'var(--color-card)' : `color-mix(in srgb, var(--color-card) 94%, ${style.soft} 6%)`,
        borderColor: tone === 'neutral' ? 'var(--color-border)' : style.border,
        flexGrow: 1,
        flexShrink: 1,
        flexBasis: `${flexBasis}px`,
      }}
    >
      <div className="flex items-center justify-between gap-1">
        <span
          className="text-[11px] font-medium uppercase tracking-wide truncate"
          style={{ color: secondaryColor }}
        >
          {label}
        </span>
        {iconNode && (
          <span className="shrink-0" style={{ color: secondaryColor }}>{iconNode}</span>
        )}
      </div>

      <span
        className="text-2xl font-bold truncate"
        style={{ color: 'var(--color-text)' }}
      >
        {valor}
      </span>

      {progressPct != null && (
        <div className="mt-1">
          <div className="h-1.5 overflow-hidden rounded-full" style={{ backgroundColor: style.track }}>
            <div className="h-full rounded-full transition-all duration-300" style={{ width: `${widthPct}%`, backgroundColor: style.fill }} />
          </div>
        </div>
      )}

      {trend && (
        <span
          className="text-xs font-medium"
          style={{
            color:
              trend.direcao === 'up'
                ? '#16a34a'
                : trend.direcao === 'down'
                  ? '#dc2626'
                  : secondaryColor,
          }}
        >
          {trend.direcao === 'up' ? '▲' : trend.direcao === 'down' ? '▼' : '—'}{' '}
          {Math.abs(trend.valor).toFixed(1)}%
        </span>
      )}
    </div>
  );
}
