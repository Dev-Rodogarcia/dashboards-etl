import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
import type { ContasAPagarOverview } from '../../../types/contasAPagar';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface ContasAPagarKpiGridProps {
  overview: ContasAPagarOverview;
}

export default function ContasAPagarKpiGrid({ overview }: ContasAPagarKpiGridProps) {
  const cards = [
    { definition: KpiDictionary.contasAPagar.valorAPagar, label: 'Valor a Pagar', valor: formatarMoeda(overview.valorAPagar) },
    { definition: KpiDictionary.contasAPagar.valorPago, label: 'Valor Pago', valor: formatarMoeda(overview.valorPago) },
    { definition: KpiDictionary.contasAPagar.saldoAberto, label: 'Saldo Aberto', valor: formatarMoeda(overview.saldoAberto) },
    { definition: KpiDictionary.contasAPagar.taxaLiquidacao, label: 'Taxa Liquidação', valor: formatarPorcentagem(overview.taxaLiquidacao) },
    { definition: KpiDictionary.contasAPagar.leadTime, label: 'Lead Time', valor: `${formatarNumero(overview.leadTimeLiquidacaoDias, 1)} dias` },
    { definition: KpiDictionary.contasAPagar.percentualConciliado, label: '% Conciliado', valor: formatarPorcentagem(overview.pctConciliado) },
  ];

  return (
    <KpiGrid count={6}>
      {cards.map((card) => (
        <TooltipKpi key={card.label} definition={card.definition}>
          <KpiCard label={card.label} valor={card.valor} />
        </TooltipKpi>
      ))}
    </KpiGrid>
  );
}
