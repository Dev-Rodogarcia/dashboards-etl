export function isParceiroLogistico(filial: string) {
  return filial.toLowerCase().includes('| parceiro');
}

export function separarFiliaisParceiros(opcoes: readonly string[]) {
  return {
    filiaisProprias: opcoes.filter((filial) => !isParceiroLogistico(filial)),
    parceirosLogisticos: opcoes.filter(isParceiroLogistico),
  };
}

export function combinarFiliaisParceiros(filiaisProprias: readonly string[], parceirosLogisticos: readonly string[]) {
  return Array.from(new Set([...filiaisProprias, ...parceirosLogisticos]));
}
