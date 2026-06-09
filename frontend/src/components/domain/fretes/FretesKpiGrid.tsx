import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
import type { FretesFaturamentoDiario, FretesOverview } from '../../../types/fretes';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';
import type { GoalTone } from '../../../utils/indicadoresGestaoVistaUi';

interface FretesKpiGridProps {
  overview: FretesOverview;
  metaFaturamento: number;
  progressoMeta: number;
  faturamentoDiario?: FretesFaturamentoDiario | null;
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

export default function FretesKpiGrid({
  overview,
  metaFaturamento,
  progressoMeta,
  faturamentoDiario,
  metasIndisponiveis,
}: FretesKpiGridProps) {
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
      <TooltipKpi definition={KpiDictionary.faturamento.totalFretes}>
        <KpiCard label="Fretes" valor={formatarNumero(overview.totalFretes)} />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.faturamentoRealizado}>
        <KpiCard label="Faturamento (Realizado)" valor={formatarMoeda(overview.receitaBruta)} />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.faturamentoLiquido}>
        <KpiCard label="Faturamento Líquido" valor={formatarMoeda(overview.valorFrete)} />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.ticketMedio}>
        <KpiCard label="Ticket Médio" valor={formatarMoeda(overview.ticketMedio)} />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.pesoTaxado}>
        <KpiCard label="Peso Taxado" valor={formatarPeso(overview.pesoTaxadoTotal)} />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.volumes}>
        <KpiCard label="Volumes" valor={formatarNumero(overview.volumesTotais)} />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.metaFaturamento}>
        <KpiCard
          label="Meta Faturamento"
          valor={faturamentoMeta.metaValue}
          helperText={faturamentoMeta.helperText}
          tone={faturamentoMeta.tone}
          helperTone={faturamentoMeta.tone}
          progressPct={faturamentoMeta.progressPct}
        />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.tendenciaPercentual}>
        <KpiCard
          label="Tendência %"
          valor={formatarPorcentagem(tendenciaPercentualExibicao)}
          valorClassName={`text-4xl font-bold ${tendenciaPercentual < 0 ? 'text-red-600' : 'text-green-600'}`}
          helperText={`Tendência: ${formatarMoeda(faturamentoDiario?.tendenciaFaturamento ?? 0)}`}
          tone={tendenciaTone}
          helperTone={tendenciaTone}
        />
      </TooltipKpi>
    </KpiGrid>
  );
}
