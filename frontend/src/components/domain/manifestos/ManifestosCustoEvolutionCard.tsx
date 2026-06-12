import { useMemo } from 'react';
import ChartWrapper from '../../charts/ChartWrapper';
import { useEchartsTheme } from '../../charts/useEchartsTheme';
import type { ManifestosCustosEvolucao } from '../../../types/manifestos';
import {
  buildManifestosCustoEvolutionOption,
  metaCustoDisponivel,
} from './manifestosCustoUi';

interface ManifestosCustoEvolutionCardProps {
  dados?: ManifestosCustosEvolucao;
  isLoading?: boolean;
}

export default function ManifestosCustoEvolutionCard({
  dados,
  isLoading,
}: ManifestosCustoEvolutionCardProps) {
  const { isDark } = useEchartsTheme();
  const option = useMemo(
    () => dados ? buildManifestosCustoEvolutionOption(dados, isDark) : {},
    [dados, isDark],
  );
  const serieVazia = !dados || dados.serieDiaria.length === 0;
  const metaIndisponivel = dados && !metaCustoDisponivel(dados);

  return (
    <ChartWrapper
      titulo="Evolução do Custo Real x Meta Diária Dinâmica"
      option={option}
      actions={metaIndisponivel ? (
        <span
          className="max-w-44 truncate text-xs font-semibold"
          style={{ color: 'var(--color-warning-text)' }}
          title={dados.observacao ?? 'Meta de custo indisponível.'}
        >
          Meta indisponível
        </span>
      ) : undefined}
      isLoading={isLoading}
      isEmpty={serieVazia}
      emptyMessage="Nenhum custo diário disponível para o período selecionado."
      altura="100%"
      className="h-full"
    />
  );
}
