import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { FaturasPorClienteOverview } from '../../../types/faturasPorCliente';
import { formatarMoeda, formatarNumero } from '../../../utils/formatadores';

interface FaturasPorClienteKpiGridProps {
  overview: FaturasPorClienteOverview;
}

export default function FaturasPorClienteKpiGrid({ overview }: FaturasPorClienteKpiGridProps) {
  return (
    <KpiGrid count={6}>
      <KpiCard label="Valor Faturado" valor={formatarMoeda(overview.valorFaturado)} />
      <KpiCard label="Reg. Faturados" valor={formatarNumero(overview.registrosFaturados)} />
      <KpiCard label="Ag. Faturamento" valor={formatarNumero(overview.aguardandoFaturamento)} />
      <KpiCard label="Tít. Atraso" valor={formatarNumero(overview.titulosEmAtraso)} />
      <KpiCard label="Prazo Médio" valor={`${formatarNumero(overview.prazoMedioDias, 1)} dias`} />
      <KpiCard label="Clientes Ativos" valor={formatarNumero(overview.clientesAtivos)} />
    </KpiGrid>
  );
}
