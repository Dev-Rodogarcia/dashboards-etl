import { Building2, Grid2X2, Heart, MapPinned } from 'lucide-react';
import type { HomeMetric } from '../../types/home';

const METRIC_ICONS = {
  dashboards: Grid2X2,
  areas: Building2,
  favoritos: Heart,
  escopo: MapPinned,
};

export default function HomeMetricGrid({ metrics }: { metrics: HomeMetric[] }) {
  return (
    <section aria-label="Resumo da Home" className="grid overflow-hidden rounded-[16px] border sm:grid-cols-2 2xl:grid-cols-4" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
      {metrics.map((metric) => {
        const Icon = METRIC_ICONS[metric.id as keyof typeof METRIC_ICONS] ?? Grid2X2;
        return (
          <article key={metric.id} className="flex min-w-0 items-center gap-2.5 border-b px-3 py-2 last:border-b-0 sm:[&:nth-child(odd)]:border-r 2xl:border-b-0 2xl:[&:not(:last-child)]:border-r" style={{ borderColor: 'var(--color-border)' }}>
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg" style={{ backgroundColor: 'rgba(33, 71, 138, 0.12)', color: 'var(--color-primary)' }}><Icon size={15} /></span>
            <div className="min-w-0">
              <div className="flex items-baseline gap-1.5"><span className="text-base font-extrabold leading-none" style={{ color: 'var(--color-text)' }}>{metric.value}</span><span className="truncate text-[11px] font-bold" title={metric.helper} style={{ color: 'var(--color-text)' }}>{metric.label}</span></div>
            </div>
          </article>
        );
      })}
    </section>
  );
}
