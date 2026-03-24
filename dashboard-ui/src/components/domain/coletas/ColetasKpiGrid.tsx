import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { ColetasOverview } from '../../../types/coletas';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';

interface ColetasKpiGridProps {
  overview: ColetasOverview;
}

export default function ColetasKpiGrid({ overview }: ColetasKpiGridProps) {
  return (
    <KpiGrid count={9}>
      <KpiCard label="Total Coletas" valor={formatarNumero(overview.totalColetas)} />
      <KpiCard label="Finalizadas" valor={formatarNumero(overview.finalizadas)} />
      <KpiCard label="Taxa Sucesso" valor={formatarPorcentagem(overview.taxaSucesso)} />
      <KpiCard label="Cancelamento %" valor={formatarPorcentagem(overview.taxaCancelamento)} />
      <KpiCard label="SLA Agendamento" valor={formatarPorcentagem(overview.slaNoAgendamento)} />
      <KpiCard label="Lead Time Médio" valor={`${formatarNumero(overview.leadTimeMedioDias, 1)} dias`} />
      <KpiCard label="Tentativas Méd." valor={formatarNumero(overview.tentativasMedias, 1)} />
      <KpiCard label="Peso Taxado" valor={formatarPeso(overview.pesoTaxadoTotal)} />
      <KpiCard label="Valor NF" valor={formatarMoeda(overview.valorNfTotal)} />
    </KpiGrid>
  );
}
