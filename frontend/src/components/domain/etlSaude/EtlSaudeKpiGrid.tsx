import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { EtlSaudeOverview } from '../../../types/etlSaude';
import { formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface EtlSaudeKpiGridProps {
  overview: EtlSaudeOverview;
}

type KpiValorTone = 'text-positive' | 'text-warning' | 'text-negative';

const KPI_VALOR_CLASS = 'text-2xl font-bold truncate';

function valorClass(tone: KpiValorTone) {
  return `${KPI_VALOR_CLASS} ${tone}`;
}

function toneTaxaSucesso(taxaSucesso: number): KpiValorTone {
  if (taxaSucesso < 90) return 'text-negative';
  if (taxaSucesso < 98) return 'text-warning';
  return 'text-positive';
}

function toneTaxaFalha(execucoesComErro: number, totalExecucoes: number): KpiValorTone {
  if (totalExecucoes <= 0 || execucoesComErro <= 0) return 'text-positive';

  const taxaFalha = (execucoesComErro * 100) / totalExecucoes;
  if (taxaFalha > 10) return 'text-negative';
  if (taxaFalha > 2) return 'text-warning';
  return 'text-positive';
}

function toneTempoMedio(segundos: number): KpiValorTone {
  if (segundos > 300) return 'text-negative';
  if (segundos > 60) return 'text-warning';
  return 'text-positive';
}

export default function EtlSaudeKpiGrid({ overview }: EtlSaudeKpiGridProps) {
  return (
    <KpiGrid count={5} singleRowDesktop>
      <KpiCard
        label="Tempo Médio (s)"
        valor={formatarNumero(overview.tempoMedioExecucaoSegundos, 2)}
        valorClassName={valorClass(toneTempoMedio(overview.tempoMedioExecucaoSegundos))}
      />
      <KpiCard
        label="Com Erro"
        valor={formatarNumero(overview.execucoesComErro)}
        valorClassName={valorClass(toneTaxaFalha(overview.execucoesComErro, overview.totalExecucoes))}
      />
      <KpiCard label="Total Execuções" valor={formatarNumero(overview.totalExecucoes)} />
      <KpiCard label="Vol. Processado" valor={formatarNumero(overview.volumeProcessadoTotal)} />
      <KpiCard
        label="Taxa Sucesso"
        valor={formatarPorcentagem(overview.taxaSucesso)}
        valorClassName={valorClass(toneTaxaSucesso(overview.taxaSucesso))}
      />
    </KpiGrid>
  );
}
