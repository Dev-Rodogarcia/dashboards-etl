import KpiCard from '../../shared/KpiCard';
import type { KPIsManifestos } from '../../../types/manifestos';
import { formatarMoeda, formatarNumero } from '../../../utils/formatadores';

interface ManifestosKpiGridProps {
  kpis?: KPIsManifestos;
  isLoading?: boolean;
}

function KpiSkeleton() {
  return (
    <div className="min-h-[92px] animate-pulse rounded-[20px] border p-3" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
      <div className="mb-4 h-3 w-24 rounded bg-slate-200" />
      <div className="h-7 w-32 rounded bg-slate-100" />
    </div>
  );
}

export default function ManifestosKpiGrid({ kpis, isLoading }: ManifestosKpiGridProps) {
  const cards = kpis
    ? [
        { label: 'Total Manifestos', valor: formatarNumero(kpis.totalManifestos) },
        { label: 'Em Trânsito', valor: formatarNumero(kpis.emTransito) },
        { label: 'Pendentes', valor: formatarNumero(kpis.pendentes) },
        { label: 'Encerrados', valor: formatarNumero(kpis.encerrados) },
        { label: 'KM Total', valor: formatarNumero(kpis.kmTotal, 0) },
        { label: 'Custo Total', valor: formatarMoeda(kpis.custoTotal) },
        { label: 'Custo/KM', valor: formatarMoeda(kpis.custoPorKm) },
        { label: 'Receita/KM', valor: formatarMoeda(kpis.receitaPorKm) },
      ]
    : [];

  return (
    <div className="mb-6 grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}>
      {isLoading
        ? Array.from({ length: 8 }, (_, index) => <KpiSkeleton key={index} />)
        : cards.map((card) => <KpiCard key={card.label} label={card.label} valor={card.valor} />)}
    </div>
  );
}
