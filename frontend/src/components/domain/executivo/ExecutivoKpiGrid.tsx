import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { ExecutivoOverview } from '../../../types/executivo';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface ExecutivoKpiGridProps {
  overview: ExecutivoOverview;
}

type KpiValorTone = 'text-positive' | 'text-warning' | 'text-negative';

const KPI_VALOR_CLASS = 'text-2xl font-bold truncate';

function valorClass(tone: KpiValorTone) {
  return `${KPI_VALOR_CLASS} ${tone}`;
}

function percentual(parte: number, total: number): number {
  if (!Number.isFinite(parte) || !Number.isFinite(total) || total <= 0) {
    return 0;
  }
  return (parte * 100) / total;
}

function toneReceita(valor: number): KpiValorTone {
  if (valor > 0) return 'text-positive';
  return 'text-warning';
}

function toneFaturamento(valorFaturado: number, receitaOperacional: number): KpiValorTone {
  if (receitaOperacional <= 0) {
    return valorFaturado > 0 ? 'text-positive' : 'text-warning';
  }

  const atingimento = percentual(valorFaturado, receitaOperacional);
  if (atingimento < 90) return 'text-negative';
  if (atingimento < 98) return 'text-warning';
  return 'text-positive';
}

function toneSaldoRelativo(valor: number, base: number, limitePositivoPct: number, limiteWarningPct: number): KpiValorTone {
  if (valor <= 0) return 'text-positive';

  const proporcao = percentual(valor, base);
  if (proporcao <= limitePositivoPct) return 'text-positive';
  if (proporcao <= limiteWarningPct) return 'text-warning';
  return 'text-negative';
}

function toneQuantidadeMenorMelhor(valor: number, limiteWarning: number): KpiValorTone {
  if (valor <= 0) return 'text-positive';
  if (valor <= limiteWarning) return 'text-warning';
  return 'text-negative';
}

function toneOcupacaoManifestos(ocupacaoPct: number): KpiValorTone {
  if (ocupacaoPct < 70) return 'text-negative';
  if (ocupacaoPct < 85) return 'text-warning';
  return 'text-positive';
}

export default function ExecutivoKpiGrid({ overview }: ExecutivoKpiGridProps) {
  const baseFinanceira = Math.max(overview.valorFaturado, overview.receitaOperacional);

  return (
    <KpiGrid count={7} singleRowDesktop>
      <KpiCard
        label="Rec. Operacional"
        valor={formatarMoeda(overview.receitaOperacional)}
        valorClassName={valorClass(toneReceita(overview.receitaOperacional))}
      />
      <KpiCard
        label="Valor Faturado"
        valor={formatarMoeda(overview.valorFaturado)}
        valorClassName={valorClass(toneFaturamento(overview.valorFaturado, overview.receitaOperacional))}
      />
      <KpiCard
        label="A Receber"
        valor={formatarMoeda(overview.saldoAReceber)}
        valorClassName={valorClass(toneSaldoRelativo(overview.saldoAReceber, baseFinanceira, 10, 25))}
      />
      <KpiCard
        label="A Pagar"
        valor={formatarMoeda(overview.saldoAPagar)}
        valorClassName={valorClass(toneSaldoRelativo(overview.saldoAPagar, baseFinanceira, 20, 50))}
      />
      <KpiCard
        label="Backlog Coletas"
        valor={formatarNumero(overview.backlogColetas)}
        valorClassName={valorClass(toneQuantidadeMenorMelhor(overview.backlogColetas, 100))}
      />
      <KpiCard
        label="Previsão Vencida"
        valor={formatarNumero(overview.cargasPrevisaoVencida)}
        valorClassName={valorClass(toneQuantidadeMenorMelhor(overview.cargasPrevisaoVencida, 10))}
      />
      <KpiCard
        label="Ocup. Manifestos"
        valor={formatarPorcentagem(overview.ocupacaoMediaManifestos)}
        valorClassName={valorClass(toneOcupacaoManifestos(overview.ocupacaoMediaManifestos))}
      />
    </KpiGrid>
  );
}
