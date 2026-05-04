import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { FretesOverview } from '../../../types/fretes';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';

interface FretesKpiGridProps {
  overview: FretesOverview;
}

export default function FretesKpiGrid({ overview }: FretesKpiGridProps) {
  return (
    <KpiGrid count={8}>
      <KpiCard label="Total de Fretes" valor={formatarNumero(overview.totalFretes)} />
      <KpiCard label="Receita Bruta" valor={formatarMoeda(overview.receitaBruta)} />
      <KpiCard label="Valor Frete" valor={formatarMoeda(overview.valorFrete)} />
      <KpiCard label="Ticket Médio" valor={formatarMoeda(overview.ticketMedio)} />
      <KpiCard label="Peso Taxado" valor={formatarPeso(overview.pesoTaxadoTotal)} />
      <KpiCard label="Volumes" valor={formatarNumero(overview.volumesTotais)} />
      <KpiCard label="CT-e Emitido" valor={formatarPorcentagem(overview.pctCteEmitido)} />
      <KpiCard label="Previsão Vencida" valor={formatarNumero(overview.fretesPrevisaoVencida)} />
    </KpiGrid>
  );
}
