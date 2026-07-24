import { Search, X } from 'lucide-react';

const focusRingClass =
  'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

export default function HomeSearch({
  value,
  onChange,
  onClear,
  compact = false,
}: {
  value: string;
  onChange: (value: string) => void;
  onClear: () => void;
  compact?: boolean;
}) {
  return (
    <label id="home-search" className={`relative block ${compact ? '' : 'mx-1'}`}>
      <span className="sr-only">Buscar dashboards, tabelas ou indicadores</span>
      <Search
        className={`pointer-events-none absolute top-1/2 -translate-y-1/2 ${compact ? 'left-3' : 'left-5'}`}
        size={compact ? 16 : 19}
        style={{ color: 'var(--color-text-muted)' }}
      />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Buscar dashboards, tabelas ou indicadores..."
        className={`${compact ? 'h-10 rounded-xl px-3 pl-10 pr-10 text-xs shadow-sm focus:shadow-md' : 'h-14 rounded-[22px] px-5 pl-14 pr-14 text-sm shadow-[0_14px_28px_rgba(15,23,42,0.06)] focus:shadow-[0_18px_36px_rgba(15,23,42,0.10)]'} w-full border transition-all duration-200 ${focusRingClass}`}
        style={{
          backgroundColor: 'var(--color-card)',
          borderColor: 'var(--color-border)',
          color: 'var(--color-text)',
        }}
      />

      {value.trim() && (
        <button
          type="button"
          onClick={onClear}
          className={`absolute top-1/2 flex -translate-y-1/2 items-center justify-center rounded-xl transition-colors hover:bg-[var(--color-bg)] ${compact ? 'right-2 h-7 w-7' : 'right-4 h-8 w-8'} ${focusRingClass}`}
          style={{ color: 'var(--color-text-muted)' }}
          aria-label="Limpar busca"
          title="Limpar busca"
        >
          <X size={15} />
        </button>
      )}
    </label>
  );
}
