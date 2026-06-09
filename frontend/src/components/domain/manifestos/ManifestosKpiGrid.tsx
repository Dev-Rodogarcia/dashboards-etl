import KpiCard from '../../shared/KpiCard';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
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
        { definition: KpiDictionary.manifestos.totalManifestos, label: 'Total Manifestos', valor: formatarNumero(kpis.totalManifestos) },
        { definition: KpiDictionary.manifestos.emTransito, label: 'Em Trânsito', valor: formatarNumero(kpis.emTransito) },
        { definition: KpiDictionary.manifestos.pendentes, label: 'Pendentes', valor: formatarNumero(kpis.pendentes) },
        { definition: KpiDictionary.manifestos.encerrados, label: 'Encerrados', valor: formatarNumero(kpis.encerrados) },
        { definition: KpiDictionary.manifestos.kmTotal, label: 'KM Total', valor: formatarNumero(kpis.kmTotal, 0) },
        { definition: KpiDictionary.manifestos.custoTotal, label: 'Custo Total', valor: formatarMoeda(kpis.custoTotal) },
        { definition: KpiDictionary.manifestos.custoPorKm, label: 'Custo/KM', valor: formatarMoeda(kpis.custoPorKm) },
        { definition: KpiDictionary.manifestos.receitaPorKm, label: 'Receita/KM', valor: formatarMoeda(kpis.receitaPorKm) },
      ]
    : [];

  return (
    <div className="mb-6 grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}>
      {isLoading
        ? Array.from({ length: 8 }, (_, index) => <KpiSkeleton key={index} />)
        : cards.map((card) => (
          <TooltipKpi key={card.label} definition={card.definition}>
            <KpiCard label={card.label} valor={card.valor} />
          </TooltipKpi>
        ))}
    </div>
  );
}
