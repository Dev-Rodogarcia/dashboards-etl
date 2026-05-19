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
          <div className="flex h-full min-h-64 items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent" style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
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
