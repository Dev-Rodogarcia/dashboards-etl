export function combinarStatusOptions(...fontes: Array<Array<string | null | undefined> | undefined>): string[] {
  const vistos = new Set<string>();

  return fontes
    .flatMap((fonte) => fonte ?? [])
    .map((valor) => valor?.trim())
    .filter((valor): valor is string => Boolean(valor))
    .filter((valor) => {
      const chave = valor.toLowerCase();
      if (vistos.has(chave)) {
        return false;
      }
      vistos.add(chave);
      return true;
    })
    .sort((a, b) => a.localeCompare(b, 'pt-BR'));
}
