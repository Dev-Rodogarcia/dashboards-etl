import KpiCard from '../../shared/KpiCard';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
import type { TrackingOverview } from '../../../types/tracking';
import { formatarMoeda, formatarNumero, formatarPeso } from '../../../utils/formatadores';

interface TrackingKpiGridProps {
  overview: TrackingOverview;
}

export default function TrackingKpiGrid({ overview }: TrackingKpiGridProps) {
  const cards = [
    { definition: KpiDictionary.tracking.totalCargas, label: 'Total de Cargas', valor: formatarNumero(overview.totalCargas) },
    { definition: KpiDictionary.tracking.emTransito, label: 'Em Trânsito', valor: formatarNumero(overview.emTransito) },
    { definition: KpiDictionary.tracking.previsaoVencida, label: 'Previsão Vencida', valor: formatarNumero(overview.previsaoVencida) },
    { definition: KpiDictionary.tracking.valorCarteira, label: 'Val. Carteira', valor: formatarMoeda(overview.valorFreteEmCarteira) },
    { definition: KpiDictionary.tracking.pesoTaxado, label: 'Peso Taxado', valor: formatarPeso(overview.pesoTaxadoTotal) },
  ];

  return (
    <div className="mb-4 grid grid-cols-1 items-stretch gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-5">
      {cards.map((card) => (
        <TooltipKpi key={card.label} definition={card.definition}>
          <KpiCard label={card.label} valor={card.valor} />
        </TooltipKpi>
      ))}
    </div>
  );
}
