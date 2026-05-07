import { useEffect, useMemo, useState } from 'react';
import type { ComponentType } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  BarChart3,
  Bell,
  BookOpen,
  Building2,
  CheckCircle2,
  ClipboardList,
  CreditCard,
  ExternalLink,
  FileText,
  HeartPulse,
  LayoutDashboard,
  MapPinned,
  Search,
  ShieldCheck,
  Star,
  Truck,
  Users,
} from 'lucide-react';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { usePermissions } from '../hooks/usePermissions';
import { DASHBOARD_NAV_ITEMS } from '../utils/accessControl';
import type { NavItem } from '../utils/accessControl';

type DashboardCategory = 'Operação' | 'Financeiro' | 'Comercial' | 'Executivo' | 'TI/ETL';

interface HomeDashboardMeta {
  category: DashboardCategory;
  description: string;
  keywords: string[];
  Icon: ComponentType<{ size?: number; className?: string }>;
  accent: string;
  priority: number;
}

interface HomeDashboardItem extends Omit<NavItem, 'description'>, HomeDashboardMeta {}

interface HomeNotice {
  id: string;
  title: string;
  body: string;
  tag: 'Novo' | 'Atenção' | 'Fixado';
  audience: string;
  date: string;
}

const CATEGORY_ORDER: DashboardCategory[] = ['Operação', 'Financeiro', 'Comercial', 'Executivo', 'TI/ETL'];
const FAVORITES_STORAGE_PREFIX = 'dashboards-etl.home.favorites';
const focusRingClass = 'outline-none focus-visible:ring-2 focus-visible:ring-[color-mix(in_srgb,var(--color-primary)_34%,transparent)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

const HOME_DASHBOARD_META: Record<string, HomeDashboardMeta> = {
  '/coletas': {
    category: 'Operação',
    description: 'Solicitações, finalizações, SLA, lead time e volume de coletas.',
    keywords: ['operacao', 'logistica', 'coletas', 'sla', 'agendamento'],
    Icon: ClipboardList,
    accent: '#2563eb',
    priority: 82,
  },
  '/manifestos': {
    category: 'Operação',
    description: 'Manifestos em trânsito, encerrados, custo, ocupação e KM.',
    keywords: ['manifestos', 'frota', 'custo', 'motorista', 'ocupacao'],
    Icon: LayoutDashboard,
    accent: '#0f766e',
    priority: 78,
  },
  '/fretes': {
    category: 'Operação',
    description: 'Receita, fretes emitidos, peso taxado, volumes e previsões.',
    keywords: ['fretes', 'receita', 'cte', 'volumes', 'previsao'],
    Icon: Truck,
    accent: '#16a34a',
    priority: 88,
  },
  '/tracking': {
    category: 'Operação',
    description: 'Localização de cargas, status de entrega e previsões vencidas.',
    keywords: ['tracking', 'localizacao', 'cargas', 'entrega', 'carteira'],
    Icon: MapPinned,
    accent: '#0891b2',
    priority: 84,
  },
  '/faturas': {
    category: 'Financeiro',
    description: 'Faturamento, recebimentos, saldo aberto, DSO e aging.',
    keywords: ['faturas', 'financeiro', 'recebimento', 'saldo', 'dso'],
    Icon: FileText,
    accent: '#7c3aed',
    priority: 90,
  },
  '/faturas-por-cliente': {
    category: 'Financeiro',
    description: 'Faturamento por cliente, registros pendentes, atraso e prazo médio.',
    keywords: ['cliente', 'faturamento', 'faturas por cliente', 'prazo', 'atraso'],
    Icon: Users,
    accent: '#9333ea',
    priority: 74,
  },
  '/contas-a-pagar': {
    category: 'Financeiro',
    description: 'Contas a pagar, liquidação, conciliação, saldo e lead time.',
    keywords: ['contas a pagar', 'fornecedor', 'liquidacao', 'conciliacao'],
    Icon: CreditCard,
    accent: '#db2777',
    priority: 86,
  },
  '/cotacoes': {
    category: 'Comercial',
    description: 'Cotações, potencial comercial, conversão e motivos de perda.',
    keywords: ['cotacoes', 'comercial', 'conversao', 'funil', 'cliente'],
    Icon: ClipboardList,
    accent: '#ea580c',
    priority: 70,
  },
  '/indicadores-gestao-a-vista': {
    category: 'Operação',
    description: 'Indicadores oficiais de performance, coletores, cubagem, indenização e corte.',
    keywords: ['gestao a vista', 'performance', 'coletores', 'cubagem', 'indenizacao', 'horarios'],
    Icon: BarChart3,
    accent: '#ca8a04',
    priority: 94,
  },
  '/executivo': {
    category: 'Executivo',
    description: 'Visão consolidada da operação, financeiro e backlog.',
    keywords: ['executivo', 'diretoria', 'consolidado', 'receita', 'backlog'],
    Icon: Activity,
    accent: '#4f46e5',
    priority: 96,
  },
  '/etl-saude': {
    category: 'TI/ETL',
    description: 'Execuções do ETL, volume processado, erros e tempo médio.',
    keywords: ['etl', 'ti', 'saude', 'execucoes', 'erros', 'monitoramento'],
    Icon: HeartPulse,
    accent: '#dc2626',
    priority: 98,
  },
};

const HOME_NOTICES: HomeNotice[] = [
  {
    id: 'gestao-vista',
    title: 'Indicadores de Gestão à Vista disponíveis',
    body: 'Performance de entrega, coletores, cubagem, indenização e horários de corte centralizados no painel operacional.',
    tag: 'Novo',
    audience: 'Operação, TI e Diretoria',
    date: 'Atualização recente',
  },
  {
    id: 'governanca',
    title: 'Acesso por setor segue permissões efetivas',
    body: 'A Home mostra somente atalhos liberados para o usuário autenticado, respeitando setor, papel e exceções individuais.',
    tag: 'Fixado',
    audience: 'Todos',
    date: 'Fixado',
  },
  {
    id: 'status-etl',
    title: 'Monitoramento do ETL em destaque',
    body: 'Acompanhe execuções, volume processado e erros no painel ETL Saúde quando a permissão estiver liberada.',
    tag: 'Atenção',
    audience: 'TI e administradores',
    date: 'Publicado hoje',
  },
];

const WHATS_NEW = [
  'Novo hub inicial com favoritos, busca e catálogo.',
  'Dashboards continuam filtrados por permissões efetivas.',
  'Favoritos ficam salvos por usuário neste navegador.',
];

function buildFavoriteKey(usuarioId?: string | null): string | null {
  return usuarioId ? `${FAVORITES_STORAGE_PREFIX}:${usuarioId}` : null;
}

function readFavoritePaths(key: string | null): string[] {
  if (!key || typeof window === 'undefined') return [];

  try {
    const raw = window.localStorage.getItem(key);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

function writeFavoritePaths(key: string | null, paths: string[]) {
  if (!key || typeof window === 'undefined') return;
  window.localStorage.setItem(key, JSON.stringify(paths));
}

function normalizeText(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

function formatRoleName(role?: string | null): string {
  if (!role) return 'Usuário comum';
  return role
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function buildHomeItem(item: NavItem): HomeDashboardItem {
  const meta = HOME_DASHBOARD_META[item.path] ?? {
    category: 'Operação' as DashboardCategory,
    description: item.description ?? 'Dashboard disponível para consulta.',
    keywords: [item.label],
    Icon: LayoutDashboard,
    accent: '#21478A',
    priority: 50,
  };

  return { ...item, ...meta, description: meta.description };
}

function InfoChip({
  label,
  value,
  Icon,
}: {
  label: string;
  value: string;
  Icon: ComponentType<{ size?: number; className?: string }>;
}) {
  return (
    <span
      className="inline-flex h-8 max-w-full items-center gap-2 border px-3 text-xs font-semibold"
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
      title={`${label}: ${value}`}
    >
      <span className="flex shrink-0" style={{ color: 'var(--color-text-muted)' }}>
        <Icon size={14} />
      </span>
      <span className="font-medium" style={{ color: 'var(--color-text-muted)' }}>{label}</span>
      <span className="truncate">{value}</span>
    </span>
  );
}

function CategoryTag({ item }: { item: HomeDashboardItem }) {
  return (
    <span
      className="inline-flex h-6 items-center px-2 text-[11px] font-bold uppercase tracking-wide"
      style={{ backgroundColor: `color-mix(in srgb, ${item.accent} 10%, var(--color-card))`, color: item.accent }}
    >
      {item.category}
    </span>
  );
}

function OpenButton({ path }: { path: string }) {
  return (
    <Link
      to={path}
      className={`inline-flex h-8 items-center justify-center gap-1.5 border px-3 text-xs font-bold transition-colors hover:bg-[var(--color-bg)] ${focusRingClass}`}
      style={{ borderColor: 'var(--color-border)', color: 'var(--color-primary)', backgroundColor: 'var(--color-card)' }}
    >
      Abrir
      <ExternalLink size={13} />
    </Link>
  );
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
      className={`flex h-8 w-8 shrink-0 items-center justify-center transition-colors hover:bg-[var(--color-bg)] ${focusRingClass}`}
      style={{ color: favorite ? '#d97706' : 'var(--color-text-muted)' }}
      aria-label={label}
      title={label}
    >
      <Star size={16} fill={favorite ? 'currentColor' : 'none'} />
    </button>
  );
}

function DashboardListRow({
  item,
  favorite,
  onToggleFavorite,
  favoriteMode = false,
}: {
  item: HomeDashboardItem;
  favorite: boolean;
  onToggleFavorite: (path: string) => void;
  favoriteMode?: boolean;
}) {
  const Icon = item.Icon;

  return (
    <div
      className="grid min-h-[72px] gap-3 border-b px-5 py-3 last:border-b-0 md:grid-cols-[minmax(250px,1fr)_150px_minmax(340px,1.4fr)_92px] md:items-center"
      style={{ borderColor: 'var(--color-border)' }}
    >
      <div className="flex min-w-0 items-center gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center" style={{ backgroundColor: `color-mix(in srgb, ${item.accent} 10%, var(--color-card))`, color: item.accent }}>
          <Icon size={17} />
        </span>
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <FavoriteButton item={item} favorite={favorite} onToggleFavorite={onToggleFavorite} />
            <h3 className="truncate text-sm font-bold" style={{ color: 'var(--color-text)' }}>
              {item.label}
            </h3>
          </div>
          <p className="mt-0.5 truncate text-[11px]" style={{ color: 'var(--color-text-muted)' }}>
            {item.permission}
          </p>
        </div>
      </div>

      <div>
        <CategoryTag item={item} />
      </div>

      <p className="text-sm leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
        {item.description}
      </p>

      <div className="flex justify-start md:justify-end">
        <OpenButton path={item.path} />
      </div>

      {favoriteMode && (
        <span className="hidden" aria-hidden="true">
          favorito
        </span>
      )}
    </div>
  );
}

export default function HomePage() {
  const { usuario } = useAutenticacao();
  const { canAccess, isAdminAcesso, isAdminPlataforma, isDesenvolvedor } = usePermissions();
  const [query, setQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<DashboardCategory | 'Todos'>('Todos');
  const [favoriteState, setFavoriteState] = useState(() => {
    const key = buildFavoriteKey(usuario?.id);
    return { key, paths: readFavoritePaths(key) };
  });

  usePageHeader({
    title: 'Home',
    description: 'Hub de acesso aos dashboards liberados para o seu perfil.',
  });

  useEffect(() => {
    const key = buildFavoriteKey(usuario?.id);
    setFavoriteState({ key, paths: readFavoritePaths(key) });
  }, [usuario?.id]);

  useEffect(() => {
    writeFavoritePaths(favoriteState.key, favoriteState.paths);
  }, [favoriteState]);

  const accessibleDashboards = useMemo(
    () =>
      DASHBOARD_NAV_ITEMS
        .map(buildHomeItem)
        .filter((item) => (item.permission ? canAccess(item.permission) : true))
        .sort((left, right) => right.priority - left.priority || left.label.localeCompare(right.label)),
    [canAccess],
  );

  const visibleCategories = useMemo(() => {
    const available = new Set(accessibleDashboards.map((item) => item.category));
    return CATEGORY_ORDER.filter((category) => available.has(category));
  }, [accessibleDashboards]);

  useEffect(() => {
    if (activeCategory !== 'Todos' && !visibleCategories.includes(activeCategory)) {
      setActiveCategory('Todos');
    }
  }, [activeCategory, visibleCategories]);

  const normalizedQuery = normalizeText(query.trim());
  const filteredDashboards = useMemo(() => {
    return accessibleDashboards.filter((item) => {
      const matchesCategory = activeCategory === 'Todos' || item.category === activeCategory;
      const searchable = normalizeText([item.label, item.description, item.category, ...item.keywords].join(' '));
      const matchesQuery = !normalizedQuery || searchable.includes(normalizedQuery);
      return matchesCategory && matchesQuery;
    });
  }, [accessibleDashboards, activeCategory, normalizedQuery]);

  const favoritePaths = favoriteState.paths;
  const favoriteItems = useMemo(
    () =>
      favoritePaths
        .map((path) => accessibleDashboards.find((item) => item.path === path))
        .filter((item): item is HomeDashboardItem => Boolean(item)),
    [accessibleDashboards, favoritePaths],
  );
  const favoritePathSet = useMemo(() => new Set(favoritePaths), [favoritePaths]);

  const filiais = usuario?.filiaisPermitidasEfetivas ?? [];
  const filiaisLabel = filiais.length > 0 ? `${filiais.length} filial(is)` : 'Acesso total';
  const adminBadge = isDesenvolvedor
    ? 'Desenvolvedor'
    : isAdminPlataforma
      ? 'Admin Plataforma'
      : isAdminAcesso
        ? 'Admin Acesso'
        : 'Usuário';

  function toggleFavorite(path: string) {
    setFavoriteState((current) => {
      const exists = current.paths.includes(path);
      const paths = exists
        ? current.paths.filter((item) => item !== path)
        : [path, ...current.paths].slice(0, 8);

      return { ...current, paths };
    });
  }

  return (
    <div className="-mx-3 -my-3 min-h-[calc(100vh-4rem)] w-[calc(100%+1.5rem)] sm:-mx-5 sm:-my-4 sm:w-[calc(100%+2.5rem)]">
      <div className="border-b px-8 py-7" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
        <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div className="min-w-0">
            <p className="mb-2 text-xs font-bold uppercase tracking-[0.16em]" style={{ color: 'var(--color-primary)' }}>
              Home
            </p>
            <h1 className="text-3xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>
              Olá, {usuario?.nome ?? 'usuário'}
            </h1>
            <p className="mt-2 text-base" style={{ color: 'var(--color-text-subtle)' }}>
              Seus dashboards liberados estão disponíveis abaixo.
            </p>
          </div>

          <div className="flex flex-wrap gap-2 xl:justify-end">
            <InfoChip label="Setor" value={usuario?.setor.nome ?? 'Perfil ativo'} Icon={Building2} />
            <InfoChip label="Perfil" value={adminBadge || formatRoleName(usuario?.papel)} Icon={ShieldCheck} />
            <InfoChip label="Filiais" value={filiaisLabel} Icon={MapPinned} />
            <InfoChip label="Dashboards" value={String(accessibleDashboards.length)} Icon={LayoutDashboard} />
          </div>
        </div>
      </div>

      <div className="border-b px-8 py-6" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
        <label className="relative mx-auto block max-w-5xl">
          <span className="sr-only">Buscar dashboards, tabelas ou indicadores</span>
          <Search className="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2" size={20} style={{ color: 'var(--color-text-muted)' }} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Buscar dashboards, tabelas ou indicadores..."
            className={`h-14 w-full border px-5 pl-14 text-base shadow-sm ${focusRingClass}`}
            style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          />
        </label>
      </div>

      <div className="grid gap-0 xl:grid-cols-[minmax(0,1fr)_380px]">
        <main className="min-w-0 border-r" style={{ borderColor: 'var(--color-border)' }}>
          <section className="border-b px-8 py-7" style={{ borderColor: 'var(--color-border)' }}>
            <div className="mb-4 flex items-end justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold" style={{ color: 'var(--color-text)' }}>
                  Minhas Tabelas Favoritas
                </h2>
                <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                  Dashboards fixados para acesso recorrente.
                </p>
              </div>
              <span className="text-xs font-bold uppercase tracking-wide" style={{ color: 'var(--color-text-muted)' }}>
                {favoriteItems.length} favorito(s)
              </span>
            </div>

            <div className="border" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
              {favoriteItems.length > 0 ? (
                favoriteItems.map((item) => (
                  <DashboardListRow
                    key={item.path}
                    item={item}
                    favorite={favoritePathSet.has(item.path)}
                    onToggleFavorite={toggleFavorite}
                    favoriteMode
                  />
                ))
              ) : (
                <div className="flex min-h-[128px] items-center justify-center px-6 py-8 text-center">
                  <div>
                    <Star className="mx-auto mb-3" size={24} style={{ color: 'var(--color-text-muted)' }} />
                    <h3 className="text-base font-bold" style={{ color: 'var(--color-text)' }}>
                      Você ainda não favoritou nenhum dashboard
                    </h3>
                    <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                      Use a estrela na lista abaixo para fixar os dashboards mais usados.
                    </p>
                  </div>
                </div>
              )}
            </div>
          </section>

          <section className="px-8 py-7">
            <div className="mb-4 flex flex-col gap-4 2xl:flex-row 2xl:items-end 2xl:justify-between">
              <div>
                <h2 className="text-xl font-bold" style={{ color: 'var(--color-text)' }}>
                  Dashboards liberados
                </h2>
                <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                  Catálogo corporativo filtrado pelo seu setor, papel e permissões efetivas.
                </p>
              </div>

              <div className="flex flex-wrap gap-2">
                {(['Todos', ...visibleCategories] as Array<DashboardCategory | 'Todos'>).map((category) => {
                  const active = activeCategory === category;

                  return (
                    <button
                      key={category}
                      type="button"
                      onClick={() => setActiveCategory(category)}
                      className={`h-8 border px-3 text-xs font-bold uppercase tracking-wide transition-colors ${focusRingClass}`}
                      style={{
                        backgroundColor: active ? 'var(--color-primary)' : 'var(--color-card)',
                        borderColor: active ? 'var(--color-primary)' : 'var(--color-border)',
                        color: active ? '#FFFFFF' : 'var(--color-text-muted)',
                      }}
                    >
                      {category}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="overflow-hidden border" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
              <div
                className="hidden min-h-10 grid-cols-[minmax(250px,1fr)_150px_minmax(340px,1.4fr)_92px] items-center gap-3 border-b px-5 text-[11px] font-bold uppercase tracking-wide md:grid"
                style={{ backgroundColor: 'color-mix(in srgb, var(--color-text) 4%, var(--color-card))', borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
              >
                <span>Nome</span>
                <span>Área</span>
                <span>Resumo</span>
                <span className="text-right">Ação</span>
              </div>

              {filteredDashboards.length > 0 ? (
                filteredDashboards.map((item) => (
                  <DashboardListRow
                    key={item.path}
                    item={item}
                    favorite={favoritePathSet.has(item.path)}
                    onToggleFavorite={toggleFavorite}
                  />
                ))
              ) : (
                <div className="px-6 py-12 text-center text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                  Nenhum dashboard encontrado para a busca atual.
                </div>
              )}
            </div>
          </section>
        </main>

        <aside className="min-w-0" style={{ backgroundColor: 'var(--color-card)' }}>
          <section className="border-b px-7 py-7" style={{ borderColor: 'var(--color-border)' }}>
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-bold" style={{ color: 'var(--color-text)' }}>
                  Comunicados internos
                </h2>
                <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                  Feed corporativo recente.
                </p>
              </div>
              <Bell size={19} style={{ color: 'var(--color-primary)' }} />
            </div>

            <div className="divide-y" style={{ borderColor: 'var(--color-border)' }}>
              {HOME_NOTICES.map((notice) => (
                <article key={notice.id} className="py-4 first:pt-0 last:pb-0">
                  <div className="mb-2 flex flex-wrap items-center gap-2">
                    <span className="inline-flex h-6 items-center px-2 text-[10px] font-bold uppercase tracking-wide" style={{ backgroundColor: 'color-mix(in srgb, var(--color-primary) 10%, var(--color-card))', color: 'var(--color-primary)' }}>
                      {notice.tag}
                    </span>
                    <span className="text-[11px]" style={{ color: 'var(--color-text-muted)' }}>
                      {notice.date}
                    </span>
                  </div>
                  <h3 className="text-sm font-bold leading-snug" style={{ color: 'var(--color-text)' }}>
                    {notice.title}
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
                    {notice.body}
                  </p>
                  <p className="mt-2 text-[11px] font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-muted)' }}>
                    Público: {notice.audience}
                  </p>
                </article>
              ))}
            </div>
          </section>

          <section className="px-7 py-7">
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-bold" style={{ color: 'var(--color-text)' }}>
                  O que há de novo
                </h2>
                <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                  Resumo curto da plataforma.
                </p>
              </div>
              <BookOpen size={19} style={{ color: 'var(--color-primary)' }} />
            </div>

            <ul className="space-y-3">
              {WHATS_NEW.map((item) => (
                <li key={item} className="flex gap-3 text-sm leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
                  <CheckCircle2 className="mt-0.5 shrink-0" size={16} style={{ color: 'var(--color-primary)' }} />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </section>
        </aside>
      </div>
    </div>
  );
}
