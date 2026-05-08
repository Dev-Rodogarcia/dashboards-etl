import type { HomeMetric } from '../../types/home';

export default function HomeMetricGrid({ metrics }: { metrics: HomeMetric[] }) {
  return (
    <section aria-label="Resumo da Home" className="grid gap-3 sm:grid-cols-2 2xl:grid-cols-4">
      {metrics.map((metric) => (
        <article
          key={metric.id}
          className="group min-h-[112px] rounded-[22px] border px-5 py-4 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_16px_34px_rgba(15,23,42,0.08)]"
          style={{
            backgroundColor: 'var(--color-card)',
            borderColor: 'color-mix(in srgb, var(--color-border) 78%, transparent)',
          }}
        >
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>
                {metric.label}
              </p>
              <p className="mt-2 text-3xl font-extrabold leading-none" style={{ color: 'var(--color-text)' }}>
                {metric.value}
              </p>
            </div>
            <span
              className="mt-1 h-2.5 w-2.5 rounded-full transition-transform duration-200 group-hover:scale-125"
              style={{ backgroundColor: 'var(--color-primary)' }}
              aria-hidden="true"
            />
          </div>
          <p className="mt-3 line-clamp-2 text-xs leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
            {metric.helper}
          </p>
        </article>
      ))}
    </section>
  );
}
