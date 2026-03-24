import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { TrackingOverview } from '../../../types/tracking';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';

interface TrackingKpiGridProps {
  overview: TrackingOverview;
}

export default function TrackingKpiGrid({ overview }: TrackingKpiGridProps) {
  return (
    <KpiGrid count={6}>
      <KpiCard label="Total de Cargas" valor={formatarNumero(overview.totalCargas)} />
      <KpiCard label="Em Trânsito" valor={formatarNumero(overview.emTransito)} />
      <KpiCard label="Previsão Vencida" valor={formatarNumero(overview.previsaoVencida)} />
      <KpiCard label="Val. Carteira" valor={formatarMoeda(overview.valorFreteEmCarteira)} />
      <KpiCard label="Peso Taxado" valor={formatarPeso(overview.pesoTaxadoTotal)} />
      <KpiCard label="% Finalizado" valor={formatarPorcentagem(overview.pctFinalizado)} />
    </KpiGrid>
  );
}
