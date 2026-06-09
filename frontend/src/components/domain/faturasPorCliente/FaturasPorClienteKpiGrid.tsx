import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
import type { FaturasPorClienteOverview } from '../../../types/faturasPorCliente';
import { formatarMoeda, formatarNumero } from '../../../utils/formatadores';

interface FaturasPorClienteKpiGridProps {
  overview: FaturasPorClienteOverview;
}

export default function FaturasPorClienteKpiGrid({ overview }: FaturasPorClienteKpiGridProps) {
  const cards = [
    { definition: KpiDictionary.faturasPorCliente.valorFaturado, label: 'Valor Faturado', valor: formatarMoeda(overview.valorFaturado) },
    { definition: KpiDictionary.faturasPorCliente.registrosFaturados, label: 'Reg. Faturados', valor: formatarNumero(overview.registrosFaturados) },
    { definition: KpiDictionary.faturasPorCliente.aguardandoFaturamento, label: 'Ag. Faturamento', valor: formatarNumero(overview.aguardandoFaturamento) },
    { definition: KpiDictionary.faturasPorCliente.titulosEmAtraso, label: 'Tít. Atraso', valor: formatarNumero(overview.titulosEmAtraso) },
    { definition: KpiDictionary.faturasPorCliente.prazoMedio, label: 'Prazo Médio', valor: `${formatarNumero(overview.prazoMedioDias, 1)} dias` },
    { definition: KpiDictionary.faturasPorCliente.clientesAtivos, label: 'Clientes Ativos', valor: formatarNumero(overview.clientesAtivos) },
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
