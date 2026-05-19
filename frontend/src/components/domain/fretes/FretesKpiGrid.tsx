import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { FretesOverview } from '../../../types/fretes';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';
import type { GoalTone } from '../../../utils/indicadoresGestaoVistaUi';

interface FretesKpiGridProps {
  overview: FretesOverview;
  metaFaturamento: number;
  metaFretes: number;
  faturamentoLiquido: number;
  progressoMeta: number;
  progressoMetaFretes: number;
  metasIndisponiveis?: boolean;
}

interface MetaKpiState {
  metaValue: string;
  helperText: string;
  tone: GoalTone;
  progressPct: number | null;
  percentValue: string;
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
      percentValue: '—',
    };
  }

  if (!Number.isFinite(meta) || meta <= 0) {
    return {
      metaValue: 'Não configurada',
      helperText: 'Meta não configurada',
      tone: 'empty',
      progressPct: null,
      percentValue: '—',
    };
  }

  const atingiu = realizado >= meta;
  return {
    metaValue: formatMeta(meta),
    helperText: atingiu ? 'Acima da meta' : 'Abaixo da meta',
    tone: atingiu ? 'positive' : 'negative',
    progressPct: progresso,
    percentValue: formatarPorcentagem(progresso),
  };
}

export default function FretesKpiGrid({
  overview,
  metaFaturamento,
  metaFretes,
  faturamentoLiquido,
  progressoMeta,
  progressoMetaFretes,
  metasIndisponiveis,
}: FretesKpiGridProps) {
  const faturamentoMeta = resolverMetaKpiState({
    realizado: overview.receitaBruta,
    meta: metaFaturamento,
    progresso: progressoMeta,
    formatMeta: formatarMoeda,
    metasIndisponiveis,
  });
  const fretesMeta = resolverMetaKpiState({
    realizado: overview.totalFretes,
    meta: metaFretes,
    progresso: progressoMetaFretes,
    formatMeta: formatarNumero,
    metasIndisponiveis,
  });

  return (
    <KpiGrid count={8}>
      <KpiCard
        label="Fretes / Meta"
        valor={formatarNumero(overview.totalFretes)}
        metaLabel="Meta"
        metaValue={fretesMeta.metaValue}
        helperText={fretesMeta.helperText}
        tone={fretesMeta.tone}
        helperTone={fretesMeta.tone}
        progressPct={fretesMeta.progressPct}
      />
      <KpiCard
        label="Faturamento / Meta"
        valor={formatarMoeda(overview.receitaBruta)}
        metaLabel="Meta"
        metaValue={faturamentoMeta.metaValue}
        helperText={faturamentoMeta.helperText}
        tone={faturamentoMeta.tone}
        helperTone={faturamentoMeta.tone}
        progressPct={faturamentoMeta.progressPct}
      />
      <KpiCard label="Faturamento Líquido" valor={formatarMoeda(faturamentoLiquido)} />
      <KpiCard label="Ticket Médio" valor={formatarMoeda(overview.ticketMedio)} />
      <KpiCard label="Peso Taxado" valor={formatarPeso(overview.pesoTaxadoTotal)} />
      <KpiCard label="Volumes" valor={formatarNumero(overview.volumesTotais)} />
      <KpiCard label="Meta Fat. %" valor={faturamentoMeta.percentValue} helperText={faturamentoMeta.helperText} tone={faturamentoMeta.tone} />
      <KpiCard label="Meta Fretes %" valor={fretesMeta.percentValue} helperText={fretesMeta.helperText} tone={fretesMeta.tone} />
    </KpiGrid>
  );
}
