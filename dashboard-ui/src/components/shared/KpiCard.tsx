function getFlexBasis(valor: string): number {
  if (valor.length > 14) return 190;
  if (valor.length > 8) return 155;
  return 110;
}

interface KpiCardProps {
  label: string;
  valor: string;
  icone?: React.ReactNode;
  trend?: {
    valor: number;
    direcao: 'up' | 'down' | 'neutral';
  };
}

export default function KpiCard({ label, valor, icone, trend }: KpiCardProps) {
  const flexBasis = getFlexBasis(valor);
  const secondaryColor = 'var(--color-text-subtle)';

  return (
    <div
      className="flex flex-col gap-1 rounded-[20px] border p-3 transition-all duration-150 hover:shadow-lg hover:-translate-y-[2px] cursor-default"
      style={{
        backgroundColor: 'var(--color-card)',
        borderColor: 'var(--color-border)',
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
        {icone && (
          <span className="shrink-0" style={{ color: secondaryColor }}>{icone}</span>
        )}
      </div>

      <span
        className="text-2xl font-bold truncate"
        style={{ color: 'var(--color-text)' }}
      >
        {valor}
      </span>

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
