import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { ExecutivoOverview } from '../../../types/executivo';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface ExecutivoKpiGridProps {
  overview: ExecutivoOverview;
}

export default function ExecutivoKpiGrid({ overview }: ExecutivoKpiGridProps) {
  return (
    <KpiGrid count={7}>
      <KpiCard label="Rec. Operacional" valor={formatarMoeda(overview.receitaOperacional)} />
      <KpiCard label="Valor Faturado" valor={formatarMoeda(overview.valorFaturado)} />
      <KpiCard label="A Receber" valor={formatarMoeda(overview.saldoAReceber)} />
      <KpiCard label="A Pagar" valor={formatarMoeda(overview.saldoAPagar)} />
      <KpiCard label="Backlog Coletas" valor={formatarNumero(overview.backlogColetas)} />
      <KpiCard label="Previsão Vencida" valor={formatarNumero(overview.cargasPrevisaoVencida)} />
      <KpiCard label="Ocup. Manifestos" valor={formatarPorcentagem(overview.ocupacaoMediaManifestos)} />
    </KpiGrid>
  );
}
