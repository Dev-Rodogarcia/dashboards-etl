import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { FaturasOverview } from '../../../types/faturas';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface FaturasKpiGridProps {
  overview: FaturasOverview;
}

export default function FaturasKpiGrid({ overview }: FaturasKpiGridProps) {
  return (
    <KpiGrid count={7}>
      <KpiCard label="Valor Faturado" valor={formatarMoeda(overview.valorFaturado)} />
      <KpiCard label="Valor Recebido" valor={formatarMoeda(overview.valorRecebido)} />
      <KpiCard label="Saldo Aberto" valor={formatarMoeda(overview.saldoAberto)} />
      <KpiCard label="Adimplência %" valor={formatarPorcentagem(overview.taxaAdimplencia)} />
      <KpiCard label="DSO Médio" valor={`${formatarNumero(overview.dsoMedioDias, 1)} dias`} />
      <KpiCard label="Tít. Atraso" valor={formatarNumero(overview.titulosEmAtraso)} />
      <KpiCard label="Clientes Ativos" valor={formatarNumero(overview.clientesAtivos)} />
    </KpiGrid>
  );
}
