import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import TooltipKpi from '../../shared/TooltipKpi';
import { KpiDictionary } from '../../../constants/kpiDictionary';
import type { CotacoesOverview } from '../../../types/cotacoes';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface CotacoesKpiGridProps {
  overview: CotacoesOverview;
}

export default function CotacoesKpiGrid({ overview }: CotacoesKpiGridProps) {
  const cards = [
    { definition: KpiDictionary.cotacoes.totalCotacoes, label: 'Total Cotações', valor: formatarNumero(overview.totalCotacoes) },
    { definition: KpiDictionary.cotacoes.valorPotencial, label: 'Potencial (R$)', valor: formatarMoeda(overview.valorPotencial) },
    { definition: KpiDictionary.cotacoes.valorConvertido, label: 'Convertido (R$)', valor: formatarMoeda(overview.valorConvertido) },
    { definition: KpiDictionary.cotacoes.freteMedio, label: 'Frete Médio', valor: formatarMoeda(overview.freteMedio) },
    { definition: KpiDictionary.cotacoes.fretePorKg, label: 'Frete/KG', valor: formatarMoeda(overview.freteKgMedio) },
    { definition: KpiDictionary.cotacoes.conversaoValor, label: 'Conversão Valor', valor: formatarPorcentagem(overview.conversaoValor) },
    { definition: KpiDictionary.cotacoes.conversaoQuantidade, label: 'Conversão Quantidade', valor: formatarPorcentagem(overview.conversaoQuantidade) },
    { definition: KpiDictionary.cotacoes.reprovacao, label: 'Reprovação %', valor: formatarPorcentagem(overview.reprovacaoPercentual) },
  ];

  return (
    <KpiGrid count={8}>
      {cards.map((card) => (
        <TooltipKpi key={card.label} definition={card.definition}>
          <KpiCard label={card.label} valor={card.valor} />
        </TooltipKpi>
      ))}
    </KpiGrid>
  );
}
