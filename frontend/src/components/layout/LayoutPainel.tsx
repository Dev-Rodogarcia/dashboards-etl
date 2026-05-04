import { Outlet } from 'react-router-dom';
import TopNav from './TopNav';

export default function LayoutPainel() {
  return (
    <div className="min-h-screen flex flex-col" style={{ backgroundColor: 'var(--color-bg)' }}>
      <TopNav />
      <main className="flex-1 w-full px-4 py-4 overflow-auto">
        <Outlet />
      </main>
      <footer
        className="mt-auto border-t px-6 py-3 flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between"
        style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-card)' }}
      >
        <div>
          <p className="text-xs font-semibold" style={{ color: 'var(--color-text-muted)' }}>
            DASHBOARDS ETL
          </p>
          <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
            Painel de indicadores operacionais e logísticos.
          </p>
        </div>
        <div className="text-right">
          <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
            Desenvolvido por{' '}
            <a
              href="https://www.linkedin.com/in/dev-lucasandrade/"
              target="_blank"
              rel="noopener noreferrer"
              className="font-medium transition-opacity hover:opacity-70"
              style={{ color: 'var(--color-primary)' }}
            >
              @valentelucass
            </a>
          </p>
          <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
            Suporte: lucasmac.dev@gmail.com
          </p>
        </div>
      </footer>
    </div>
  );
}
