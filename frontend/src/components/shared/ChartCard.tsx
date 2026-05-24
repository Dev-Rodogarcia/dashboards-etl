import type { ReactNode } from 'react';

interface ChartCardProps {
  titulo: string;
  children: ReactNode;
  actions?: ReactNode;
  isLoading?: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  erro?: string | null;
  className?: string;
  contentClassName?: string;
}

function ChartSkeleton() {
  return (
    <div className="flex h-full min-h-64 flex-col justify-end gap-3" aria-hidden="true">
      <div className="flex h-44 items-end gap-3">
        {[52, 88, 64, 112, 76, 132, 96].map((height, index) => (
          <div key={index} className="flex-1 rounded-t-md bg-slate-200/80" style={{ height }} />
        ))}
      </div>
      <div className="h-3 w-full rounded bg-slate-100" />
      <div className="flex gap-2">
        <div className="h-2 w-24 rounded bg-slate-200/80" />
        <div className="h-2 w-20 rounded bg-slate-100" />
        <div className="h-2 w-28 rounded bg-slate-100" />
      </div>
    </div>
  );
}

export default function ChartCard({
  titulo,
  children,
  actions,
  isLoading,
  isEmpty,
  emptyMessage,
  erro,
  className = '',
  contentClassName = '',
}: ChartCardProps) {
  return (
    <div
      className={`flex h-full min-h-0 flex-col rounded-[20px] border p-4 shadow-sm ${className}`}
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
    >
      <div className="mb-3 flex shrink-0 items-center justify-between gap-3 border-b pb-3" style={{ borderColor: 'var(--color-border)' }}>
        <h3 className="min-w-0 truncate text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
          {titulo}
        </h3>
        {actions ? <div className="shrink-0">{actions}</div> : null}
      </div>

      <div className={`min-h-0 flex-1 overflow-hidden ${contentClassName}`}>
        {isLoading ? (
          <div className="h-full animate-pulse">
            <ChartSkeleton />
          </div>
        ) : erro ? (
          <div className="flex h-full min-h-64 items-center justify-center rounded-xl border border-dashed px-6 text-center text-sm" style={{ borderColor: '#dc2626', backgroundColor: 'rgba(220, 38, 38, 0.08)', color: '#dc2626' }}>
            {erro}
          </div>
        ) : (!isLoading && isEmpty) ? (
          <div className="flex h-full min-h-64 items-center justify-center text-sm" style={{ color: 'var(--color-text-muted)' }}>
            {emptyMessage ?? 'Nenhum dado disponivel para o periodo selecionado.'}
          </div>
        ) : (
          children
        )}
      </div>
    </div>
  );
}
