import KpiCard from '../../shared/KpiCard';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
import type { FaturamentoDiario, FaturamentoOverview } from '../../../types/faturamento';
import { formatarMoeda, formatarNumero, formatarPorcentagem, formatarPeso } from '../../../utils/formatadores';

interface FaturamentoKpiGridProps {
  overview: FaturamentoOverview;
  metaFaturamento: number;
  progressoMeta: number;
  faturamentoDiario?: FaturamentoDiario | null;
  metasIndisponiveis?: boolean;
}

type KpiValorTone = 'text-positive' | 'text-warning' | 'text-negative';

const KPI_VALOR_CLASS = 'text-2xl font-bold truncate';
const KPI_VALOR_DESTAQUE_CLASS = 'text-4xl font-bold truncate';

interface MetaKpiState {
  metaValue: string;
  helperText: string;
  valorTone: KpiValorTone;
  progressPct: number | null;
}

function valorClass(tone: KpiValorTone, baseClassName = KPI_VALOR_CLASS) {
  return `${baseClassName} ${tone}`;
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
      valorTone: 'text-negative',
      progressPct: null,
    };
  }

  if (!Number.isFinite(meta) || meta <= 0) {
    return {
      metaValue: 'Não configurada',
      helperText: 'Meta não configurada',
      valorTone: 'text-warning',
      progressPct: null,
    };
  }

  const atingiu = realizado >= meta;
  return {
    metaValue: formatMeta(meta),
    helperText: atingiu ? 'Acima da meta' : 'Abaixo da meta',
    valorTone: atingiu ? 'text-positive' : 'text-negative',
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
  const tendenciaValorTone = tendenciaPercentual < 0 ? 'text-negative' : 'text-positive';

  return (
    <div className="mb-4 grid grid-cols-1 items-stretch gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <TooltipKpi definition={KpiDictionary.faturamento.totalMinutas}>
        <KpiCard label="Minutas" valor={formatarNumero(overview.totalFretes)} />
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
          valorClassName={valorClass(faturamentoMeta.valorTone)}
          helperText={faturamentoMeta.helperText}
          progressPct={faturamentoMeta.progressPct}
        />
      </TooltipKpi>
      <TooltipKpi definition={KpiDictionary.faturamento.tendenciaPercentual}>
        <KpiCard
          label="Tendência %"
          valor={formatarPorcentagem(tendenciaPercentualExibicao)}
          valorClassName={valorClass(tendenciaValorTone, KPI_VALOR_DESTAQUE_CLASS)}
          helperText={`Tendência: ${formatarMoeda(faturamentoDiario?.tendenciaFaturamento ?? 0)}`}
        />
      </TooltipKpi>
    </div>
  );
}
