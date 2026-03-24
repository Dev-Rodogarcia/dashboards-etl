import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { EtlSaudeOverview } from '../../../types/etlSaude';
import { formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface EtlSaudeKpiGridProps {
  overview: EtlSaudeOverview;
}

export default function EtlSaudeKpiGrid({ overview }: EtlSaudeKpiGridProps) {
  return (
    <KpiGrid count={5}>
      <KpiCard label="Tempo Médio (s)" valor={formatarNumero(overview.tempoMedioExecucaoSegundos, 2)} />
      <KpiCard label="Com Erro" valor={formatarNumero(overview.execucoesComErro)} />
      <KpiCard label="Total Execuções" valor={formatarNumero(overview.totalExecucoes)} />
      <KpiCard label="Vol. Processado" valor={formatarNumero(overview.volumeProcessadoTotal)} />
      <KpiCard label="Taxa Sucesso" valor={formatarPorcentagem(overview.taxaSucesso)} />
    </KpiGrid>
  );
}
