import type { ReactNode } from 'react';

interface ChartCardProps {
  titulo: string;
  children: ReactNode;
  isLoading?: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  erro?: string | null;
  className?: string;
}

export default function ChartCard({
  titulo,
  children,
  isLoading,
  isEmpty,
  emptyMessage,
  erro,
  className = '',
}: ChartCardProps) {
  return (
    <div
      className={`rounded-[20px] border p-4 shadow-sm ${className}`}
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
    >
      <h3 className="mb-3 text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
        {titulo}
      </h3>

      {isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent" style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
        </div>
      ) : erro ? (
        <div className="flex h-64 items-center justify-center rounded-xl border border-dashed border-red-200 bg-red-50 px-6 text-center text-sm text-red-700">
          {erro}
        </div>
      ) : (!isLoading && isEmpty) ? (
        <div className="flex h-64 items-center justify-center text-sm" style={{ color: 'var(--color-text-muted)' }}>
          {emptyMessage ?? 'Nenhum dado disponivel para o periodo selecionado.'}
        </div>
      ) : (
        children
      )}
    </div>
  );
}
