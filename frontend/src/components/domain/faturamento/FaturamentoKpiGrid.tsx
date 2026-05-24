import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { FaturamentoDiario, FaturamentoOverview } from '../../../types/faturamento';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';
import type { GoalTone } from '../../../utils/indicadoresGestaoVistaUi';

interface FaturamentoKpiGridProps {
  overview: FaturamentoOverview;
  metaFaturamento: number;
  progressoMeta: number;
  faturamentoDiario?: FaturamentoDiario | null;
  metasIndisponiveis?: boolean;
}

interface MetaKpiState {
  metaValue: string;
  helperText: string;
  tone: GoalTone;
  progressPct: number | null;
}

function resolverMetaKpiState({
  realizado,
  meta,
  progresso,
  formatMeta,
  metasIndisponiveis,
}: {
  realizado: number;
  meta: number;
  progresso: number;
  formatMeta: (value: number) => string;
  metasIndisponiveis?: boolean;
}): MetaKpiState {
  if (metasIndisponiveis) {
    return {
      metaValue: '—',
      helperText: 'Metas indisponíveis (API offline)',
      tone: 'error',
      progressPct: null,
    };
  }

  if (!Number.isFinite(meta) || meta <= 0) {
    return {
      metaValue: 'Não configurada',
      helperText: 'Meta não configurada',
      tone: 'empty',
      progressPct: null,
    };
  }

  const atingiu = realizado >= meta;
  return {
    metaValue: formatMeta(meta),
    helperText: atingiu ? 'Acima da meta' : 'Abaixo da meta',
    tone: atingiu ? 'positive' : 'negative',
    progressPct: progresso,
  };
}

export default function FaturamentoKpiGrid({
  overview,
  metaFaturamento,
  progressoMeta,
  faturamentoDiario,
  metasIndisponiveis,
}: FaturamentoKpiGridProps) {
  const faturamentoMeta = resolverMetaKpiState({
    realizado: overview.receitaBruta,
    meta: metaFaturamento,
    progresso: progressoMeta,
    formatMeta: formatarMoeda,
    metasIndisponiveis,
  });
  const tendenciaPercentual = faturamentoDiario?.tendenciaPercentual ?? 0;
  const tendenciaPercentualExibicao = tendenciaPercentual * 100;
  const tendenciaTone = tendenciaPercentual < 0 ? 'negative' : 'positive';

  return (
    <KpiGrid count={8}>
      <KpiCard label="Minutas" valor={formatarNumero(overview.totalFretes)} />
      <KpiCard label="Faturamento (Realizado)" valor={formatarMoeda(overview.receitaBruta)} />
      <KpiCard label="Faturamento Líquido" valor={formatarMoeda(overview.valorFrete)} />
      <KpiCard label="Ticket Médio" valor={formatarMoeda(overview.ticketMedio)} />
      <KpiCard label="Peso Taxado" valor={formatarPeso(overview.pesoTaxadoTotal)} />
      <KpiCard label="Volumes" valor={formatarNumero(overview.volumesTotais)} />
      <KpiCard
        label="Meta Faturamento"
        valor={faturamentoMeta.metaValue}
        helperText={faturamentoMeta.helperText}
        tone={faturamentoMeta.tone}
        helperTone={faturamentoMeta.tone}
        progressPct={faturamentoMeta.progressPct}
      />
      <KpiCard
        label="Tendência %"
        valor={formatarPorcentagem(tendenciaPercentualExibicao)}
        valorClassName={`text-4xl font-bold ${tendenciaPercentual < 0 ? 'text-red-600' : 'text-green-600'}`}
        helperText={`Tendência: ${formatarMoeda(faturamentoDiario?.tendenciaFaturamento ?? 0)}`}
        tone={tendenciaTone}
        helperTone={tendenciaTone}
      />
    </KpiGrid>
  );
}
