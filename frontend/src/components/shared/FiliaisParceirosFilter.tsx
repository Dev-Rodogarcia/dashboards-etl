import { useMemo } from 'react';
import AsyncMultiSelect from './AsyncMultiSelect';
import { separarFiliaisParceiros } from '../../utils/filiais';

interface FiliaisParceirosFilterProps {
  opcoes: string[];
  filiaisSelecionadas: string[];
  parceirosSelecionados: string[];
  onFiliaisChange: (valores: string[]) => void;
  onParceirosChange: (valores: string[]) => void;
  isLoading?: boolean;
  filialLabel?: string;
  parceiroLabel?: string;
}

export default function FiliaisParceirosFilter({
  opcoes,
  filiaisSelecionadas,
  onFiliaisChange,
  isLoading,
  filialLabel = 'Filiais',
}: FiliaisParceirosFilterProps) {
  const { filiaisProprias } = useMemo(() => separarFiliaisParceiros(opcoes), [opcoes]);

  return (
    <AsyncMultiSelect
      label={filialLabel}
      opcoes={filiaisProprias}
      selecionados={filiaisSelecionadas}
      onChange={onFiliaisChange}
      isLoading={isLoading}
    />
  );
}
