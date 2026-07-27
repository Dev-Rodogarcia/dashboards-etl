import { Building2, LayoutDashboard, MapPinned, ShieldCheck } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import HomeMetricGrid from './HomeMetricGrid';
import type { HomeMetric } from '../../types/home';

function cleanDisplayName(nome: string) {
  return nome.split('|')[0]?.trim() || nome;
}

function InfoChip({
  label,
  value,
  Icon,
}: {
  label: string;
  value: string;
  Icon: LucideIcon;
}) {
  return (
    <span
      className="inline-flex min-w-0 items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold"
      style={{
        backgroundColor: 'var(--color-bg)',
        borderColor: 'var(--color-border)',
        color: 'var(--color-text)',
      }}
      title={`${label}: ${value}`}
    >
      <Icon className="shrink-0" size={14} style={{ color: 'var(--color-primary)' }} />
      <span className="shrink-0" style={{ color: 'var(--color-primary)' }}>
        {label}
      </span>
      <span className="truncate">{value}</span>
    </span>
  );
}

export default function HomeHero({
  nome,
  roleLabel,
  setorLabel,
  filiaisLabel,
  dashboardsLabel,
  metrics,
  search,
}: {
  nome: string;
  roleLabel: string;
  setorLabel: string;
  filiaisLabel: string;
  dashboardsLabel: string;
  metrics: HomeMetric[];
  search: ReactNode;
}) {
  const displayName = cleanDisplayName(nome);

  return (
    <section
      className="w-full rounded-[20px] border px-4 py-3 shadow-sm sm:px-5"
      style={{
        backgroundColor: 'var(--color-card)',
        borderColor: 'var(--color-border)',
      }}
    >
      <div className="min-w-0">
        <div className="grid gap-3 lg:grid-cols-[minmax(15rem,18rem)_minmax(20rem,1fr)_auto] lg:items-center">
          <div className="lg:border-r lg:pr-5" style={{ borderColor: 'var(--color-border)' }}>
            <h1 className="text-base font-extrabold leading-tight sm:text-lg" style={{ color: 'var(--color-text)' }}>
              Bem-vindo, {displayName}
            </h1>
            <p className="mt-1 hidden text-xs font-medium sm:block" style={{ color: 'var(--color-text-muted)' }}>
              Seu espaço de trabalho está pronto para hoje.
            </p>
          </div>

          <div className="min-w-0 lg:pl-2">{search}</div>

          <div className="grid min-w-0 grid-cols-1 gap-2 sm:grid-cols-2 lg:justify-end">
            <InfoChip label="Setor" value={setorLabel} Icon={Building2} />
            <InfoChip label="Perfil" value={roleLabel} Icon={ShieldCheck} />
            <InfoChip label="Filiais" value={filiaisLabel} Icon={MapPinned} />
            <InfoChip label="Dashboards" value={dashboardsLabel} Icon={LayoutDashboard} />
          </div>
        </div>

        <div className="mt-2 border-t pt-2" style={{ borderColor: 'var(--color-border)' }}>
          <HomeMetricGrid metrics={metrics} />
        </div>
      </div>
    </section>
  );
}
