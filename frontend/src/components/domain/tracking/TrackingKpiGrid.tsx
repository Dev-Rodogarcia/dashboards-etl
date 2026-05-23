import KpiCard from '../../shared/KpiCard';
import type { TrackingOverview } from '../../../types/tracking';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';

interface TrackingKpiGridProps {
  overview: TrackingOverview;
}

export default function TrackingKpiGrid({ overview }: TrackingKpiGridProps) {
  const cards = [
    { label: 'Total de Cargas', valor: formatarNumero(overview.totalCargas) },
    { label: 'Em Trânsito', valor: formatarNumero(overview.emTransito) },
    { label: 'Previsão Vencida', valor: formatarNumero(overview.previsaoVencida) },
    { label: 'Val. Carteira', valor: formatarMoeda(overview.valorFreteEmCarteira) },
    { label: 'Peso Taxado', valor: formatarPeso(overview.pesoTaxadoTotal) },
    { label: '% Finalizado', valor: formatarPorcentagem(overview.pctFinalizado) },
  ];

  return (
    <div className="mb-4 grid grid-cols-1 items-stretch gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-6">
      {cards.map((card) => (
        <KpiCard key={card.label} label={card.label} valor={card.valor} />
      ))}
    </div>
  );
}
