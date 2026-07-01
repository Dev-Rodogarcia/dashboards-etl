import AsyncMultiSelect from '../shared/AsyncMultiSelect';

interface FiliaisPermitidasSplitSelectProps {
  opcoes: string[];
  selecionadas: string[];
  onChange: (filiais: string[]) => void;
  isLoading?: boolean;
}

function isParceiroLogistico(filial: string) {
  return filial.toLowerCase().includes('| parceiro');
}

function combinarSelecoes(filiaisProprias: string[], parceirosLogisticos: string[]) {
  return Array.from(new Set([...filiaisProprias, ...parceirosLogisticos]));
}

export default function FiliaisPermitidasSplitSelect({
  opcoes,
  selecionadas,
  onChange,
  isLoading,
}: FiliaisPermitidasSplitSelectProps) {
  const filiaisProprias = opcoes.filter((filial) => !isParceiroLogistico(filial));
  const parceirosLogisticos = opcoes.filter(isParceiroLogistico);
  const filiaisPropriasSelecionadas = selecionadas.filter((filial) => !isParceiroLogistico(filial));
  const parceirosLogisticosSelecionados = selecionadas.filter(isParceiroLogistico);

  function atualizarFiliaisProprias(proximasFiliaisProprias: string[]) {
    onChange(combinarSelecoes(proximasFiliaisProprias, parceirosLogisticosSelecionados));
  }

  function atualizarParceirosLogisticos(proximosParceirosLogisticos: string[]) {
    onChange(combinarSelecoes(filiaisPropriasSelecionadas, proximosParceirosLogisticos));
  }

  return (
    <div className="my-12 flex w-full max-w-4xl flex-col gap-3 md:flex-row">
      <div className="min-w-0 flex-0.4">
        <AsyncMultiSelect
          label="Filiais Próprias Permitidas"
          opcoes={filiaisProprias}
          selecionados={filiaisPropriasSelecionadas}
          onChange={atualizarFiliaisProprias}
          placeholder="Selecione as filiais próprias"
          isLoading={isLoading}
        />
      </div>

      <div className="min-w-0 flex-0.4">
        <AsyncMultiSelect
          label="Parceiros Logísticos Permitidos"
          opcoes={parceirosLogisticos}
          selecionados={parceirosLogisticosSelecionados}
          onChange={atualizarParceirosLogisticos}
          placeholder="Selecione os parceiros logísticos"
          isLoading={isLoading}
        />
      </div>
    </div>
  );
}
