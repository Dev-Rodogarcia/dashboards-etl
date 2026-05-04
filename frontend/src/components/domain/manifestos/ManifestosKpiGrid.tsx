import KpiCard from '../../shared/KpiCard';
import KpiGrid from '../../shared/KpiGrid';
import type { ManifestosOverview } from '../../../types/manifestos';
import { formatarMoeda, formatarNumero, formatarPorcentagem } from '../../../utils/formatadores';

interface ManifestosKpiGridProps {
  overview: ManifestosOverview;
}

export default function ManifestosKpiGrid({ overview }: ManifestosKpiGridProps) {
  return (
    <KpiGrid count={8}>
      <KpiCard label="Total Manifestos" valor={formatarNumero(overview.totalManifestos)} />
      <KpiCard label="Em Trânsito" valor={formatarNumero(overview.emTransito)} />
      <KpiCard label="Encerrados" valor={formatarNumero(overview.encerrados)} />
      <KpiCard label="KM Total" valor={formatarNumero(overview.kmTotal, 0)} />
      <KpiCard label="Custo Total" valor={formatarMoeda(overview.custoTotal)} />
      <KpiCard label="Custo/KM" valor={formatarMoeda(overview.custoPorKm)} />
      <KpiCard label="Ocup. Peso %" valor={formatarPorcentagem(overview.ocupacaoPesoMediaPct)} />
      <KpiCard label="Ocup. Cubagem %" valor={formatarPorcentagem(overview.ocupacaoCubagemMediaPct)} />
    </KpiGrid>
  );
}
