import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { ContasAPagarOverview } from '../../../types/contasAPagar';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface ContasAPagarKpiGridProps {
  overview: ContasAPagarOverview;
}

export default function ContasAPagarKpiGrid({ overview }: ContasAPagarKpiGridProps) {
  return (
    <KpiGrid count={6}>
      <KpiCard label="Valor a Pagar" valor={formatarMoeda(overview.valorAPagar)} />
      <KpiCard label="Valor Pago" valor={formatarMoeda(overview.valorPago)} />
      <KpiCard label="Saldo Aberto" valor={formatarMoeda(overview.saldoAberto)} />
      <KpiCard label="Taxa Liquidação" valor={formatarPorcentagem(overview.taxaLiquidacao)} />
      <KpiCard label="Lead Time" valor={`${formatarNumero(overview.leadTimeLiquidacaoDias, 1)} dias`} />
      <KpiCard label="% Conciliado" valor={formatarPorcentagem(overview.pctConciliado)} />
    </KpiGrid>
  );
}
