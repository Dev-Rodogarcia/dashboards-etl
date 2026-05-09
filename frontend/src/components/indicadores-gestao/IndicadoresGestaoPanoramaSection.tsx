import type { ReactNode } from 'react';
import { getGoalToneStyle, type GoalTone } from '../../utils/indicadoresGestaoVistaUi';

export interface PanoramaOperacionalItem {
  id: string;
  title: string;
  value: string;
  statusLabel: string;
  tone: GoalTone;
  progressPct?: number | null;
  detail: string;
  alertDetail?: string;
  severityScore?: number;
  icon?: ReactNode;
}

interface IndicadoresGestaoPanoramaSectionProps {
  title?: string;
  description?: string;
  items: PanoramaOperacionalItem[];
}

function formatarProgresso(progressPct?: number | null): string {
  if (progressPct == null) {
    return '—';
  }
  const valor = Math.max(0, Math.min(progressPct, 100));
  return `${valor.toLocaleString('pt-BR', { maximumFractionDigits: valor % 1 === 0 ? 0 : 1 })}%`;
}

export default function IndicadoresGestaoPanoramaSection({
  title = 'Panorama Operacional',
  description = 'Resumo visual dos cinco indicadores para leitura imediata em tela cheia.',
  items,
}: IndicadoresGestaoPanoramaSectionProps) {
  const alertItems = items
    .filter((item) => item.tone === 'warning' || item.tone === 'negative')
    .sort((left, right) => (right.severityScore ?? 0) - (left.severityScore ?? 0)
      || (left.progressPct ?? 100) - (right.progressPct ?? 100)
      || left.title.localeCompare(right.title))
    .slice(0, 3);

  return (
    <section
      aria-label={title}
      className="mb-7 rounded-[26px] border px-5 py-4 shadow-sm xl:mb-8 xl:min-h-[400px] xl:px-6 xl:py-5"
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
    >
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold" style={{ color: 'var(--color-text)' }}>
            {title}
          </h2>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            {description}
          </p>
        </div>
        <div className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
          Comparativo visual das metas oficiais
        </div>
      </div>

      <div className="space-y-2.5">
        {items.map((item) => {
          const style = getGoalToneStyle(item.tone);
          const progressPct = Math.max(0, Math.min(item.progressPct ?? 0, 100));

          return (
            <article
              key={item.id}
              className="rounded-[20px] border px-4 py-[14px] transition-colors xl:px-5 xl:py-4"
              style={{
                backgroundColor: 'var(--color-bg)',
                borderColor: item.tone === 'neutral' ? 'var(--color-border)' : style.border,
              }}
            >
              <div className="grid gap-2.5 xl:grid-cols-[minmax(0,1.55fr)_auto_auto_minmax(320px,1.95fr)] xl:items-center">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    {item.icon ? (
                      <span className="shrink-0" style={{ color: style.text }}>
                        {item.icon}
                      </span>
                    ) : null}
                    <span className="truncate text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
                      {item.title}
                    </span>
                  </div>
                </div>

                <div className="text-left xl:text-right">
                  <div className="text-xl font-bold leading-none" style={{ color: 'var(--color-text)' }}>
                    {item.value}
                  </div>
                </div>

                <div className="xl:justify-self-start">
                  <span
                    className="inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide"
                    style={{ backgroundColor: style.badgeBg, borderColor: style.badgeBorder, color: style.badgeText }}
                  >
                    {item.statusLabel}
                  </span>
                </div>

                <div className="min-w-0">
                  <div className="mb-1.5 flex items-center justify-between gap-3 text-[11px]" style={{ color: 'var(--color-text-subtle)' }}>
                    <span className="truncate">{item.detail}</span>
                    <span className="shrink-0 font-semibold" style={{ color: style.text }}>
                      {formatarProgresso(item.progressPct)}
                    </span>
                  </div>
                  {item.progressPct != null ? (
                    <div className="h-1.5 overflow-hidden rounded-full" style={{ backgroundColor: style.track }}>
                      <div
                        className="h-full rounded-full transition-all duration-300"
                        style={{ width: `${progressPct}%`, backgroundColor: style.fill }}
                      />
                    </div>
                  ) : null}
                </div>
              </div>
            </article>
          );
        })}
      </div>

      <div
        className="mt-4 rounded-[22px] border px-4 py-3.5 xl:px-5 xl:py-4"
        style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}
      >
        <div className="mb-2.5 flex flex-wrap items-start justify-between gap-2">
          <div>
            <h3 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
              Atenções do Período
            </h3>
            <p className="mt-1 text-xs" style={{ color: 'var(--color-text-subtle)' }}>
              Os maiores gaps operacionais do recorte filtrado.
            </p>
          </div>
          <div className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
            Top 3 gaps operacionais
          </div>
        </div>

        {alertItems.length > 0 ? (
          <div className="grid gap-2 xl:grid-cols-3">
            {alertItems.map((item) => {
              const style = getGoalToneStyle(item.tone);

              return (
                <article
                  key={`${item.id}-attention`}
                  className="rounded-[18px] border px-3.5 py-3"
                  style={{
                    backgroundColor: 'var(--color-bg)',
                    borderColor: style.border,
                  }}
                >
                  <div className="mb-2 flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="truncate text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
                        {item.title}
                      </div>
                    </div>
                    <span
                      className="inline-flex shrink-0 rounded-full border px-2 py-1 text-[10px] font-semibold uppercase tracking-wide"
                      style={{ backgroundColor: style.badgeBg, borderColor: style.badgeBorder, color: style.badgeText }}
                    >
                      {item.statusLabel}
                    </span>
                  </div>
                  <div className="text-xs leading-5" style={{ color: 'var(--color-text-subtle)' }}>
                    {item.alertDetail ?? item.detail}
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div
            className="rounded-[18px] border border-dashed px-3.5 py-3 text-xs"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-subtle)' }}
          >
            Nenhum alerta crítico no período.
          </div>
        )}
      </div>
    </section>
  );
}
