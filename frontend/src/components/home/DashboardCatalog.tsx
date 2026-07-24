import { ExternalLink, Lock, Star } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { HomeDashboardCategory, HomeDashboardFilter, HomeDashboardItem } from '../../types/home';

const focusRingClass =
  'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

function hexToRgba(hex: string, alpha: number) {
  const normalized = hex.replace('#', '');
  if (normalized.length !== 6) {
    return 'rgba(71, 85, 105, 0.14)';
  }

  const red = Number.parseInt(normalized.slice(0, 2), 16);
  const green = Number.parseInt(normalized.slice(2, 4), 16);
  const blue = Number.parseInt(normalized.slice(4, 6), 16);

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

function FavoriteButton({
  item,
  favorite,
  onToggleFavorite,
}: {
  item: HomeDashboardItem;
  favorite: boolean;
  onToggleFavorite: (path: string) => void;
}) {
  const label = favorite ? `Remover ${item.label} dos favoritos` : `Favoritar ${item.label}`;

  return (
    <button
      type="button"
      onClick={() => onToggleFavorite(item.path)}
      className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg transition-colors hover:bg-[var(--color-bg)] ${focusRingClass}`}
      style={{ color: favorite ? '#d97706' : 'var(--color-text-muted)' }}
      aria-label={label}
      title={label}
    >
      <Star size={16} fill={favorite ? 'currentColor' : 'none'} />
    </button>
  );
}

function OpenButton({ item }: { item: HomeDashboardItem }) {
  if (!item.isAccessible) {
    return (
      <span
        className="inline-flex h-8 items-center justify-center rounded-lg px-2 text-xs font-bold leading-none"
        style={{
          backgroundColor: 'rgba(220, 38, 38, 0.12)',
          color: '#dc2626',
        }}
        aria-label={`${item.label} sem acesso para este perfil`}
      >
        <Lock className="shrink-0" size={14} strokeWidth={2.4} />
      </span>
    );
  }

  return (
    <Link
      to={item.path}
      className={`inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border text-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary)] hover:text-white ${focusRingClass}`}
      style={{ borderColor: 'var(--color-primary)', backgroundColor: 'var(--color-card)' }}
      aria-label={`Abrir ${item.label}`}
      title={`Abrir ${item.label}`}
    >
      <ExternalLink size={13} />
    </Link>
  );
}

function DashboardRow({
  item,
  favorite,
  onToggleFavorite,
}: {
  item: HomeDashboardItem;
  favorite: boolean;
  onToggleFavorite: (path: string) => void;
}) {
  const Icon = item.Icon;
  const locked = !item.isAccessible;
  const textColor = locked ? 'var(--color-text-muted)' : 'var(--color-text)';

  return (
    <div
      className={`flex min-h-[76px] min-w-0 items-center gap-3 rounded-2xl border p-3 ${
        locked ? '' : 'transition-colors hover:-translate-y-0.5 hover:shadow-sm'
      }`}
      style={{
        backgroundColor: locked ? 'var(--color-bg)' : 'var(--color-card)',
        borderColor: 'var(--color-border)',
      }}
      aria-disabled={locked}
    >
      <div className="flex min-w-0 flex-1 items-center gap-3" title={item.description}>
        <span
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border"
          style={{
            backgroundColor: locked ? 'rgba(71, 85, 105, 0.14)' : hexToRgba(item.accent, 0.14),
            borderColor: locked ? '#334155' : item.accent,
            color: locked ? '#475569' : item.accent,
          }}
        >
          <Icon size={17} />
        </span>
        <div className="min-w-0">
          <h3 className="truncate text-sm font-bold" style={{ color: textColor }}>{item.label}</h3>
          {locked && (
            <p className="mt-1 text-[11px] font-semibold uppercase" style={{ color: 'var(--color-text-muted)' }}>
              Disponível mediante liberação de acesso
            </p>
          )}
        </div>
      </div>
      {item.isAccessible && <FavoriteButton item={item} favorite={favorite} onToggleFavorite={onToggleFavorite} />}
      <OpenButton item={item} />
    </div>
  );
}

export default function DashboardCatalog({
  dashboards,
  favorites,
  categories,
  activeCategory,
  onCategoryChange,
  onToggleFavorite,
  title = 'Dashboards liberados',
  subtitle = 'Catálogo corporativo com itens liberados e bloqueados por perfil.',
}: {
  dashboards: HomeDashboardItem[];
  favorites: Set<string>;
  categories: HomeDashboardCategory[];
  activeCategory: HomeDashboardFilter;
  onCategoryChange: (category: HomeDashboardFilter) => void;
  onToggleFavorite: (path: string) => void;
  title?: string;
  subtitle?: string;
}) {
  const emptyMessage = activeCategory === 'Favoritos'
    ? 'Nenhum dashboard favorito encontrado.'
    : 'Nenhum dashboard encontrado para a busca atual.';

  return (
    <section>
      <div className="mb-4 flex flex-col gap-4 2xl:flex-row 2xl:items-end 2xl:justify-between">
        <div>
          <h2 className="text-xl font-bold" style={{ color: 'var(--color-text)' }}>
            {title}
          </h2>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            {subtitle}
          </p>
        </div>

        {categories.length > 0 && (
          <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrar dashboards por área">
            {(['Todos', 'Favoritos', ...categories] as HomeDashboardFilter[]).map((category) => {
              const active = activeCategory === category;

              return (
                <button
                  key={category}
                  type="button"
                  onClick={() => onCategoryChange(category)}
                  className={`h-8 rounded-full border px-3 text-xs font-bold uppercase transition-all duration-200 hover:-translate-y-0.5 ${focusRingClass}`}
                  style={{
                    backgroundColor: active ? 'var(--color-primary)' : 'var(--color-card)',
                    borderColor: active ? 'var(--color-primary)' : 'var(--color-border)',
                    color: active ? '#FFFFFF' : 'var(--color-text-muted)',
                  }}
                  aria-pressed={active}
                >
                  {category}
                </button>
              );
            })}
          </div>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-2 2xl:grid-cols-3">
        {dashboards.length > 0 ? (
          dashboards.map((item) => (
            <DashboardRow
              key={item.path}
              item={item}
              favorite={favorites.has(item.path)}
              onToggleFavorite={onToggleFavorite}
            />
          ))
        ) : (
          <div className="col-span-full rounded-[20px] border px-6 py-12 text-center text-sm" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-subtle)' }}>
            {emptyMessage}
          </div>
        )}
      </div>
    </section>
  );
}
