const IDENTIFICADOR_FILIAL_PROPRIA = 'RODOGARCIA';

function normalizarFilial(filial: string) {
  return filial.trim().toUpperCase();
}

/**
 * A dimensão analítica também contém transportadoras e prestadores usados
 * nas operações. Filiais próprias sempre carregam a razão Rodogarcia, tanto
 * no label completo quanto no alias "TR RODOGARCIA | <sigla>".
 */
export function isFilialPropria(filial: string) {
  return normalizarFilial(filial).includes(IDENTIFICADOR_FILIAL_PROPRIA);
}

export function isParceiroLogistico(filial: string) {
  return !isFilialPropria(filial);
}

export function separarFiliaisParceiros(opcoes: readonly string[]) {
  return {
    filiaisProprias: opcoes.filter(isFilialPropria),
    parceirosLogisticos: opcoes.filter(isParceiroLogistico),
  };
}

export function combinarFiliaisParceiros(filiaisProprias: readonly string[], parceirosLogisticos: readonly string[]) {
  return Array.from(new Set([...filiaisProprias, ...parceirosLogisticos]));
}
