import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { CotacoesOverview } from '../../../types/cotacoes';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface CotacoesKpiGridProps {
  overview: CotacoesOverview;
}

export default function CotacoesKpiGrid({ overview }: CotacoesKpiGridProps) {
  return (
    <KpiGrid count={8}>
      <KpiCard label="Total Cotações" valor={formatarNumero(overview.totalCotacoes)} />
      <KpiCard label="Potencial (R$)" valor={formatarMoeda(overview.valorPotencial)} />
      <KpiCard label="Frete Médio" valor={formatarMoeda(overview.freteMedio)} />
      <KpiCard label="Frete/KG" valor={formatarMoeda(overview.freteKgMedio)} />
      <KpiCard label="Conv. CT-e %" valor={formatarPorcentagem(overview.taxaConversaoCte)} />
      <KpiCard label="Conv. NFS-e %" valor={formatarPorcentagem(overview.taxaConversaoNfse)} />
      <KpiCard label="Reprovação %" valor={formatarPorcentagem(overview.taxaReprovacao)} />
      <KpiCard label="Conv. Médio (h)" valor={formatarNumero(overview.tempoMedioConversaoHoras, 1)} />
    </KpiGrid>
  );
}
