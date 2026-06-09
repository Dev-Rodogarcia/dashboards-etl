export function getKpiFlexBasis(valor: string): number {
  if (valor.length > 14) return 190;
  if (valor.length > 8) return 155;
  return 110;
}
