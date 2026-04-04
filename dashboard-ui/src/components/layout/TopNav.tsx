import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useTheme } from 'next-themes';
import { ShieldCheck, ChevronDown, Menu, X } from 'lucide-react';
import { useAutenticacao } from '../../contexts/AutenticacaoContext';
import { usePermissions } from '../../hooks/usePermissions';
import { ADMIN_NAV_ITEMS, DASHBOARD_NAV_ITEMS } from '../../utils/accessControl';
import type { NavItem } from '../../utils/accessControl';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '../ui/dropdown-menu';

const focusRingClass = 'outline-none focus-visible:ring-2 focus-visible:ring-[color-mix(in_srgb,var(--color-primary)_30%,transparent)]';
const DESKTOP_NAV_ITEM_GAP = 4;
const DESKTOP_NAV_TOGGLE_GAP = 8;

type DesktopNavEntry =
  | {
      key: string;
      type: 'dashboard';
      item: NavItem;
    }
  | {
      key: 'admin';
      type: 'admin';
    };

function measureDesktopNavWidth(entries: DesktopNavEntry[], itemWidths: Record<string, number>) {
  return entries.reduce((total, entry, index) => {
    const width = itemWidths[entry.key] ?? 0;
    return total + width + (index > 0 ? DESKTOP_NAV_ITEM_GAP : 0);
  }, 0);
}

function buildCollapsedDesktopNavEntries(
  entries: DesktopNavEntry[],
  activeEntryKey: string | null,
  itemWidths: Record<string, number>,
  availableWidth: number,
) {
  if (entries.length === 0 || availableWidth <= 0) {
    return entries.slice(0, 1);
  }

  const visibleEntries: DesktopNavEntry[] = [];

  for (const entry of entries) {
    const nextEntries = [...visibleEntries, entry];
    if (measureDesktopNavWidth(nextEntries, itemWidths) > availableWidth && visibleEntries.length > 0) {
      break;
    }

    visibleEntries.push(entry);
  }

  if (activeEntryKey == null || visibleEntries.some((entry) => entry.key === activeEntryKey)) {
    return visibleEntries;
  }

  const activeEntry = entries.find((entry) => entry.key === activeEntryKey);
  if (!activeEntry) {
    return visibleEntries;
  }

  const pinnedEntries = [...visibleEntries];
  while (pinnedEntries.length > 0) {
    const candidateEntries = [...pinnedEntries.slice(0, -1), activeEntry];
    if (measureDesktopNavWidth(candidateEntries, itemWidths) <= availableWidth) {
      return candidateEntries;
    }

    pinnedEntries.pop();
  }

  return [activeEntry];
}

function SunIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="5" />
      <line x1="12" y1="1" x2="12" y2="3" />
      <line x1="12" y1="21" x2="12" y2="23" />
      <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
      <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
      <line x1="1" y1="12" x2="3" y2="12" />
      <line x1="21" y1="12" x2="23" y2="12" />
      <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
      <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>
  );
}

function MobileNavSection({
  title,
  items,
  onNavigate,
}: {
  title: string;
  items: NavItem[];
  onNavigate: () => void;
}) {
  const headingId = `${title.toLowerCase().replace(/\s+/g, '-')}-heading`;

  return (
    <section aria-labelledby={headingId}>
      <h2
        id={headingId}
        className="mb-3 px-1 text-[11px] font-semibold uppercase tracking-[0.16em]"
        style={{ color: 'var(--color-text-muted)' }}
      >
        {title}
      </h2>

      <div className="space-y-2">
        {items.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            onClick={onNavigate}
            className={({ isActive }) =>
              `block rounded-2xl border px-4 py-3 transition-all duration-150 hover:bg-[var(--color-bg)] ${focusRingClass} ${
                isActive ? 'shadow-sm' : ''
              }`
            }
            style={({ isActive }) => ({
              backgroundColor: isActive ? 'var(--color-bg)' : 'transparent',
              borderColor: isActive ? 'var(--color-primary)' : 'var(--color-border)',
              color: isActive ? 'var(--color-primary)' : 'var(--color-text)',
            })}
          >
            {({ isActive }) => (
              <>
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm font-semibold">{item.label}</span>
                  {isActive && (
                    <span
                      className="rounded-full px-2 py-0.5 text-[10px] font-bold leading-none text-white"
                      style={{ backgroundColor: 'var(--color-primary)' }}
                    >
                      Atual
                    </span>
                  )}
                </div>

                {item.description && (
                  <p className="mt-1 text-xs leading-relaxed" style={{ color: 'var(--color-text-muted)' }}>
                    {item.description}
                  </p>
                )}
              </>
            )}
          </NavLink>
        ))}
      </div>
    </section>
  );
}

export default function TopNav() {
  const { usuario, logout } = useAutenticacao();
  const navigate = useNavigate();
  const currentLocation = useLocation();
  const { theme, setTheme } = useTheme();
  const { canAccess, isAdminAcesso, isAdminPlataforma } = usePermissions();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [desktopNavHasOverflow, setDesktopNavHasOverflow] = useState(false);
  const [desktopNavItemWidths, setDesktopNavItemWidths] = useState<Record<string, number>>({});
  const [desktopNavToggleWidth, setDesktopNavToggleWidth] = useState(0);
  const [desktopNavCollapsedWidth, setDesktopNavCollapsedWidth] = useState(0);
  const hamburgerButtonRef = useRef<HTMLButtonElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const desktopNavShellRef = useRef<HTMLDivElement>(null);
  const desktopNavMeasureRef = useRef<HTMLDivElement>(null);
  const desktopNavToggleMeasureRef = useRef<HTMLButtonElement>(null);
  const wasMenuOpenRef = useRef(false);
  const previousPathnameRef = useRef(currentLocation.pathname);
  const panelId = useId();
  const drawerTitleId = `${panelId}-title`;

  const dashboardsVisiveis = DASHBOARD_NAV_ITEMS.filter((item) =>
    item.permission ? canAccess(item.permission) : true,
  );
  const adminItems = isAdminAcesso ? ADMIN_NAV_ITEMS : [];
  const desktopNavEntries: DesktopNavEntry[] = [
    ...dashboardsVisiveis.map((item) => ({
      key: item.path,
      type: 'dashboard' as const,
      item,
    })),
    ...(isAdminAcesso ? [{ key: 'admin' as const, type: 'admin' as const }] : []),
  ];
  const desktopNavMeasurementKey = desktopNavEntries.map((entry) => entry.key).join('|');
  const mobileSections = [
    { title: 'Dashboards', items: dashboardsVisiveis },
    ...(adminItems.length > 0 ? [{ title: 'Administração', items: adminItems }] : []),
  ].filter((section) => section.items.length > 0);

  const adminBadge = isAdminPlataforma
    ? 'Admin Plataforma'
    : isAdminAcesso
      ? 'Admin Acesso'
      : null;
  const isAdminRoute = currentLocation.pathname.startsWith('/admin');
  const isDarkTheme = theme === 'dark';
  const themeToggleLabel = isDarkTheme ? 'Alternar para modo claro' : 'Alternar para modo escuro';
  const hamburgerLabel = isMobileMenuOpen ? 'Fechar menu de navegação' : 'Abrir menu de navegação';
  const activeDesktopNavEntryKey = isAdminRoute ? 'admin' : currentLocation.pathname;
  const desktopNavVisibleEntries = desktopNavHasOverflow
    ? buildCollapsedDesktopNavEntries(
        desktopNavEntries,
        activeDesktopNavEntryKey,
        desktopNavItemWidths,
        desktopNavCollapsedWidth,
      )
    : desktopNavEntries;
  const desktopNavVisibleKeys = new Set(desktopNavVisibleEntries.map((entry) => entry.key));
  const desktopNavOverflowEntries = desktopNavEntries.filter((entry) => !desktopNavVisibleKeys.has(entry.key));
  const desktopNavHasHiddenAdmin = desktopNavOverflowEntries.some((entry) => entry.type === 'admin');
  const desktopNavOverflowDashboards = desktopNavOverflowEntries.filter((entry) => entry.type === 'dashboard');

  function renderDesktopNavEntry(entry: DesktopNavEntry, measureOnly = false) {
    if (entry.type === 'dashboard') {
      if (measureOnly) {
        return (
          <span
            key={entry.key}
            data-desktop-nav-measure-item="true"
            data-desktop-nav-key={entry.key}
            className="inline-flex whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium"
          >
            {entry.item.label}
          </span>
        );
      }

      return (
        <NavLink
          key={entry.key}
          to={entry.item.path}
          className={({ isActive }) =>
            `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors duration-150 ${focusRingClass} ${
              isActive ? 'text-white' : 'hover:bg-[var(--color-bg)]'
            }`
          }
          style={({ isActive }) =>
            isActive
              ? { backgroundColor: 'var(--color-primary)' }
              : { color: 'var(--color-text-muted)' }
          }
        >
          {entry.item.label}
        </NavLink>
      );
    }

    if (measureOnly) {
      return (
        <span
          key={entry.key}
          data-desktop-nav-measure-item="true"
          data-desktop-nav-key={entry.key}
          className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium"
        >
          <ShieldCheck size={14} />
          Admin
          <ChevronDown size={12} />
        </span>
      );
    }

    return (
      <DropdownMenu key={entry.key}>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            className={`flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors duration-150 hover:bg-[var(--color-bg)] ${focusRingClass}`}
            style={isAdminRoute
              ? {
                  backgroundColor: 'var(--color-primary)',
                  color: '#FFFFFF',
                }
              : {
                  color: 'var(--color-text-muted)',
                }}
          >
            <ShieldCheck size={14} />
            Admin
            <ChevronDown size={12} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          {ADMIN_NAV_ITEMS.map((item) => (
            <DropdownMenuItem key={item.path} asChild>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  `flex w-full flex-col ${isActive ? 'font-semibold' : ''}`
                }
                style={({ isActive }) => ({
                  color: isActive ? 'var(--color-primary)' : 'var(--color-text)',
                })}
              >
                <span className="font-medium">{item.label}</span>
                {item.description && (
                  <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                    {item.description}
                  </span>
                )}
              </NavLink>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  useLayoutEffect(() => {
    const shell = desktopNavShellRef.current;
    const measure = desktopNavMeasureRef.current;
    const toggle = desktopNavToggleMeasureRef.current;

    if (!shell || !measure || desktopNavMeasurementKey.length === 0) {
      setDesktopNavHasOverflow(false);
      setDesktopNavItemWidths({});
      setDesktopNavToggleWidth(0);
      setDesktopNavCollapsedWidth(0);
      return;
    }

    const shellElement = shell;
    const measureElement = measure;
    let frame = 0;

    function measureDesktopNav() {
      const shellWidth = Math.ceil(shellElement.getBoundingClientRect().width);
      const toggleWidth = toggle ? Math.ceil(toggle.getBoundingClientRect().width) : 0;
      const itemElements = Array.from(measureElement.querySelectorAll<HTMLElement>('[data-desktop-nav-measure-item="true"]'));
      const measuredWidths = Object.fromEntries(
        itemElements.map((element) => [
          element.dataset.desktopNavKey ?? '',
          Math.ceil(element.getBoundingClientRect().width),
        ]),
      );
      const totalWidth = itemElements.reduce((total, element, index) => {
        return total + Math.ceil(element.getBoundingClientRect().width) + (index > 0 ? DESKTOP_NAV_ITEM_GAP : 0);
      }, 0);

      setDesktopNavItemWidths(measuredWidths);
      setDesktopNavToggleWidth(toggleWidth);
      setDesktopNavCollapsedWidth(Math.max(shellWidth - toggleWidth - DESKTOP_NAV_TOGGLE_GAP, 0));
      setDesktopNavHasOverflow(shellWidth > 0 && totalWidth > shellWidth);
    }

    function scheduleMeasure() {
      window.cancelAnimationFrame(frame);
      frame = window.requestAnimationFrame(measureDesktopNav);
    }

    scheduleMeasure();

    const observer = new ResizeObserver(scheduleMeasure);
    observer.observe(shellElement);
    observer.observe(measureElement);
    if (toggle) {
      observer.observe(toggle);
    }

    return () => {
      window.cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, [desktopNavMeasurementKey]);

  useEffect(() => {
    const previousPathname = previousPathnameRef.current;
    previousPathnameRef.current = currentLocation.pathname;

    if (!isMobileMenuOpen || previousPathname === currentLocation.pathname) {
      return;
    }

    const frame = window.requestAnimationFrame(() => {
      setIsMobileMenuOpen(false);
    });

    return () => {
      window.cancelAnimationFrame(frame);
    };
  }, [currentLocation.pathname, isMobileMenuOpen]);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 860px)');

    function handleMediaChange(event: MediaQueryListEvent) {
      if (!event.matches) {
        setIsMobileMenuOpen(false);
      }
    }

    mediaQuery.addEventListener('change', handleMediaChange);
    return () => {
      mediaQuery.removeEventListener('change', handleMediaChange);
    };
  }, []);

  useEffect(() => {
    if (!isMobileMenuOpen) {
      return;
    }

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [isMobileMenuOpen]);

  useEffect(() => {
    if (!isMobileMenuOpen) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsMobileMenuOpen(false);
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isMobileMenuOpen]);

  useEffect(() => {
    if (isMobileMenuOpen) {
      wasMenuOpenRef.current = true;
      const frame = window.requestAnimationFrame(() => {
        closeButtonRef.current?.focus();
      });

      return () => {
        window.cancelAnimationFrame(frame);
      };
    }

    if (wasMenuOpenRef.current) {
      hamburgerButtonRef.current?.focus();
      wasMenuOpenRef.current = false;
    }
  }, [isMobileMenuOpen]);

  async function handleLogout() {
    setIsMobileMenuOpen(false);
    await logout();
    navigate('/login', { replace: true });
  }

  function toggleTheme() {
    setTheme(isDarkTheme ? 'light' : 'dark');
  }

  const mobileDrawer = typeof document !== 'undefined'
    ? createPortal(
        <AnimatePresence>
          {isMobileMenuOpen && (
            <>
              <motion.div
                className="fixed inset-0 z-40 bg-slate-950/45 backdrop-blur-[2px]"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.2 }}
                onClick={() => setIsMobileMenuOpen(false)}
                aria-hidden="true"
              />

              <motion.aside
                id={panelId}
                role="dialog"
                aria-modal="true"
                aria-labelledby={drawerTitleId}
                className="top-nav__drawer-panel fixed inset-y-0 right-0 z-50 flex flex-col overflow-hidden border-l shadow-2xl"
                initial={{ x: '100%' }}
                animate={{ x: 0 }}
                exit={{ x: '100%' }}
                transition={{ type: 'spring', damping: 28, stiffness: 320 }}
                style={{
                  backgroundColor: 'var(--color-card)',
                  borderColor: 'var(--color-border)',
                }}
              >
                <div
                  className="flex items-center justify-between gap-3 border-b px-5 py-4"
                  style={{ borderColor: 'var(--color-border)' }}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <div
                      className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border"
                      style={{
                        backgroundColor: 'var(--color-bg)',
                        borderColor: 'var(--color-border)',
                      }}
                    >
                      <img
                        src="/logo.png"
                        alt=""
                        className="h-5 w-auto object-contain transition-all duration-200 dark:brightness-0 dark:invert"
                      />
                    </div>
                    <div className="min-w-0">
                      <p id={drawerTitleId} className="truncate text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
                        Navegação
                      </p>
                      <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                        Links disponíveis para o seu acesso
                      </p>
                    </div>
                  </div>

                  <button
                    ref={closeButtonRef}
                    type="button"
                    onClick={() => setIsMobileMenuOpen(false)}
                    className={`flex h-10 w-10 items-center justify-center rounded-xl transition-colors duration-150 hover:bg-[var(--color-bg)] ${focusRingClass}`}
                    style={{ color: 'var(--color-text-muted)' }}
                    aria-label="Fechar menu de navegação"
                  >
                    <X size={18} />
                  </button>
                </div>

                <div className="flex flex-1 flex-col overflow-hidden px-5 pb-5 pt-5">
                  <nav className="top-nav__drawer-nav flex-1" aria-label="Navegação principal do painel">
                    <div className="space-y-6 pr-1">
                      {mobileSections.map((section) => (
                        <MobileNavSection
                          key={section.title}
                          title={section.title}
                          items={section.items}
                          onNavigate={() => setIsMobileMenuOpen(false)}
                        />
                      ))}
                    </div>
                  </nav>

                  <div
                    className="top-nav__drawer-footer mt-5 border-t pt-4"
                    style={{ borderColor: 'var(--color-border)' }}
                  >
                    <button
                      type="button"
                      onClick={() => void handleLogout()}
                      className={`w-full rounded-2xl px-4 py-3 text-sm font-semibold text-red-500 transition-all duration-150 hover:bg-red-500 hover:text-white ${focusRingClass}`}
                    >
                      Sair
                    </button>
                  </div>
                </div>
              </motion.aside>
            </>
          )}
        </AnimatePresence>,
        document.body,
      )
    : null;

  return (
    <>
      <header
        className="top-nav sticky top-0 z-50 flex h-14 items-center justify-between gap-4 border-b px-5 shadow-sm"
        style={{
          backgroundColor: 'var(--color-card)',
          borderColor: 'var(--color-border)',
        }}
      >
        <div
          className="top-nav__logo-wrap flex shrink-0 items-center border-r pr-4"
          style={{ borderColor: 'var(--color-border)' }}
        >
          <img
            src="/logo.png"
            alt="Logo da empresa"
            className="top-nav__logo h-7 w-auto object-contain transition-all duration-200 dark:brightness-0 dark:invert"
          />
        </div>

        <div ref={desktopNavShellRef} className="top-nav__desktop-nav relative flex min-w-0 flex-1 items-center gap-2">
          <nav
            className="flex min-w-0 flex-1 items-center gap-1 overflow-hidden"
            aria-label="Navegação principal"
          >
            {desktopNavVisibleEntries.map((entry) => renderDesktopNavEntry(entry))}
          </nav>

          {desktopNavHasOverflow && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  className={`inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors duration-150 hover:bg-[var(--color-bg)] ${focusRingClass}`}
                  style={{
                    color: 'var(--color-text-muted)',
                    minWidth: desktopNavToggleWidth > 0 ? `${desktopNavToggleWidth}px` : undefined,
                  }}
                >
                  Mostrar mais
                  <ChevronDown size={12} />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start">
                {desktopNavOverflowDashboards.map((entry) => (
                  <DropdownMenuItem key={entry.key} asChild>
                    <NavLink
                      to={entry.item.path}
                      className={({ isActive }) =>
                        `flex w-full flex-col ${isActive ? 'font-semibold' : ''}`
                      }
                      style={({ isActive }) => ({
                        color: isActive ? 'var(--color-primary)' : 'var(--color-text)',
                      })}
                    >
                      <span className="font-medium">{entry.item.label}</span>
                    </NavLink>
                  </DropdownMenuItem>
                ))}

                {desktopNavHasHiddenAdmin && desktopNavOverflowDashboards.length > 0 && (
                  <DropdownMenuSeparator
                    className="mx-2 my-1 h-px"
                    style={{ backgroundColor: 'var(--color-border)' }}
                  />
                )}

                {desktopNavHasHiddenAdmin && ADMIN_NAV_ITEMS.map((item) => (
                  <DropdownMenuItem key={item.path} asChild>
                    <NavLink
                      to={item.path}
                      className={({ isActive }) =>
                        `flex w-full flex-col ${isActive ? 'font-semibold' : ''}`
                      }
                      style={({ isActive }) => ({
                        color: isActive ? 'var(--color-primary)' : 'var(--color-text)',
                      })}
                    >
                      <span className="font-medium">{item.label}</span>
                      {item.description && (
                        <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                          {item.description}
                        </span>
                      )}
                    </NavLink>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          )}

          <div
            ref={desktopNavMeasureRef}
            aria-hidden="true"
            className="pointer-events-none absolute left-0 top-0 -z-10 flex items-center gap-1 opacity-0"
          >
            {desktopNavEntries.map((entry) => renderDesktopNavEntry(entry, true))}
          </div>

          <button
            ref={desktopNavToggleMeasureRef}
            type="button"
            aria-hidden="true"
            tabIndex={-1}
            className="pointer-events-none absolute left-0 top-0 -z-10 inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium opacity-0"
          >
            Mostrar mais
            <ChevronDown size={12} />
          </button>
        </div>

        <div className="top-nav__right flex shrink-0 items-center gap-3">
          <div className="top-nav__desktop-info flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>
                {usuario?.setor.nome}
              </span>
              {adminBadge && (
                <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-700">
                  {adminBadge}
                </span>
              )}
            </div>

            <div className="top-nav__divider h-5 w-px" style={{ backgroundColor: 'var(--color-border)' }} />
          </div>

          <button
            type="button"
            onClick={toggleTheme}
            title={themeToggleLabel}
            aria-label={themeToggleLabel}
            className={`top-nav__theme-button flex h-8 w-8 items-center justify-center rounded-lg transition-colors duration-150 hover:bg-[var(--color-bg)] ${focusRingClass}`}
            style={{ color: 'var(--color-text-muted)' }}
          >
            {isDarkTheme ? <SunIcon /> : <MoonIcon />}
          </button>

          <button
            type="button"
            onClick={() => void handleLogout()}
            className={`top-nav__desktop-logout rounded-lg px-3 py-1.5 text-sm font-medium text-red-500 transition-all duration-150 hover:bg-red-500 hover:text-white ${focusRingClass}`}
          >
            Sair
          </button>

          <button
            ref={hamburgerButtonRef}
            type="button"
            onClick={() => setIsMobileMenuOpen((current) => !current)}
            aria-label={hamburgerLabel}
            aria-expanded={isMobileMenuOpen}
            aria-controls={panelId}
            className={`top-nav__mobile-menu-button flex h-8 w-8 items-center justify-center rounded-lg transition-colors duration-150 hover:bg-[var(--color-bg)] ${focusRingClass}`}
            style={{ color: 'var(--color-text-muted)' }}
          >
            {isMobileMenuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>
      </header>

      {mobileDrawer}
    </>
  );
}
