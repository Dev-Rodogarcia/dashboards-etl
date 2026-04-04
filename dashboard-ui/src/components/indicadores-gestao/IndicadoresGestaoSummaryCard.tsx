import type { ReactNode } from 'react';
import { getGoalToneStyle, type GoalTone } from '../../utils/indicadoresGestaoVistaUi';

interface IndicadoresGestaoSummaryCardProps {
  title: string;
  description: string;
  value: string;
  detail: string;
  goalLabel: string;
  statusLabel: string;
  tone: GoalTone;
  progressPct?: number | null;
  icon?: ReactNode;
}

export default function IndicadoresGestaoSummaryCard({
  title,
  description,
  value,
  detail,
  goalLabel,
  statusLabel,
  tone,
  progressPct,
  icon,
}: IndicadoresGestaoSummaryCardProps) {
  const style = getGoalToneStyle(tone);
  const widthPct = Math.max(0, Math.min(progressPct ?? 0, 100));

  return (
    <div
      className="rounded-[20px] border p-4 shadow-sm transition-colors"
      style={{
        backgroundColor: tone === 'neutral' ? 'var(--color-card)' : `color-mix(in srgb, var(--color-card) 92%, ${style.soft} 8%)`,
        borderColor: tone === 'neutral' ? 'var(--color-border)' : style.border,
      }}
    >
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
            {title}
          </h3>
          <p className="mt-1 text-xs" style={{ color: 'var(--color-text-subtle)' }}>
            {description}
          </p>
        </div>
        {icon ? <span style={{ color: 'var(--color-text-subtle)' }}>{icon}</span> : null}
      </div>

      <div className="mb-2 flex flex-wrap items-center gap-2">
        <span className="text-2xl font-bold" style={{ color: 'var(--color-text)' }}>
          {value}
        </span>
        <span
          className="rounded-full px-2 py-1 text-[11px] font-semibold uppercase tracking-wide"
          style={{ backgroundColor: style.bg, color: style.text }}
        >
          {statusLabel}
        </span>
      </div>

      <div className="text-xs font-medium" style={{ color: 'var(--color-text-subtle)' }}>
        {goalLabel}
      </div>
      <div className="mt-1 text-xs" style={{ color: 'var(--color-text-muted)' }}>
        {detail}
      </div>
      {progressPct != null ? (
        <div className="mt-3">
          <div className="mb-1 flex items-center justify-between text-[11px]" style={{ color: 'var(--color-text-subtle)' }}>
            <span>Cobertura da meta</span>
            <span>{widthPct.toLocaleString('pt-BR', { maximumFractionDigits: widthPct % 1 === 0 ? 0 : 1 })}%</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full" style={{ backgroundColor: style.track }}>
            <div className="h-full rounded-full transition-all duration-300" style={{ width: `${widthPct}%`, backgroundColor: style.fill }} />
          </div>
        </div>
      ) : null}
    </div>
  );
}
