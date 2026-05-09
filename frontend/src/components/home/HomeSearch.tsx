import { Search, X } from 'lucide-react';

const focusRingClass =
  'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

export default function HomeSearch({
  value,
  onChange,
  onClear,
}: {
  value: string;
  onChange: (value: string) => void;
  onClear: () => void;
}) {
  return (
    <label id="home-search" className="relative block">
      <span className="sr-only">Buscar dashboards, tabelas ou indicadores</span>
      <Search
        className="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2"
        size={19}
        style={{ color: 'var(--color-text-muted)' }}
      />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Buscar dashboards, tabelas ou indicadores..."
        className={`h-14 w-full rounded-[22px] border px-5 pl-14 pr-14 text-sm shadow-[0_14px_28px_rgba(15,23,42,0.06)] transition-all duration-200 focus:shadow-[0_18px_36px_rgba(15,23,42,0.10)] ${focusRingClass}`}
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
          className={`absolute right-4 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-xl transition-colors hover:bg-[var(--color-bg)] ${focusRingClass}`}
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
