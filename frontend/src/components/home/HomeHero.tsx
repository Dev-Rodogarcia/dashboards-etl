import { Building2, LayoutDashboard, MapPinned, ShieldCheck } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

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
        backgroundColor: 'color-mix(in srgb, var(--color-bg) 72%, var(--color-card))',
        borderColor: 'var(--color-border)',
        color: 'var(--color-text)',
      }}
      title={`${label}: ${value}`}
    >
      <Icon className="shrink-0" size={14} style={{ color: 'var(--color-text-muted)' }} />
      <span className="shrink-0" style={{ color: 'var(--color-text-muted)' }}>
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
}: {
  nome: string;
  roleLabel: string;
  setorLabel: string;
  filiaisLabel: string;
  dashboardsLabel: string;
}) {
  const displayName = cleanDisplayName(nome);

  return (
    <section
      className="w-full rounded-[22px] border px-5 py-5 shadow-sm sm:px-6"
      style={{
        backgroundColor: 'var(--color-card)',
        borderColor: 'var(--color-border)',
      }}
    >
      <div className="min-w-0">
        <h1 className="text-2xl font-extrabold leading-tight sm:text-[1.8rem]" style={{ color: 'var(--color-text)' }}>
          Home
        </h1>

        <div className="mt-3 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          <p className="shrink-0 text-sm font-semibold sm:text-base" style={{ color: 'var(--color-text-muted)' }}>
            Olá, {displayName} <span aria-hidden="true">|</span> {roleLabel}
          </p>

          <div className="grid min-w-0 flex-1 grid-cols-1 gap-2 sm:grid-cols-2 xl:flex xl:flex-wrap xl:justify-end">
            <InfoChip label="Setor" value={setorLabel} Icon={Building2} />
            <InfoChip label="Perfil" value={roleLabel} Icon={ShieldCheck} />
            <InfoChip label="Filiais" value={filiaisLabel} Icon={MapPinned} />
            <InfoChip label="Dashboards" value={dashboardsLabel} Icon={LayoutDashboard} />
          </div>
        </div>
      </div>
    </section>
  );
}
