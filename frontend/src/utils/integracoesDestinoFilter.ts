export interface ItemComSistemaDestino {
  sistemaDestino: string;
}

function normalizarDestino(destino: string) {
  return destino.trim().toUpperCase();
}

export function filtrarPorDestinosSelecionados<T extends ItemComSistemaDestino>(
  itens: T[],
  destinosSelecionados: string[],
) {
  if (destinosSelecionados.length === 0) {
    return itens;
  }

  const destinos = new Set(destinosSelecionados.map(normalizarDestino));
  return itens.filter((item) => destinos.has(normalizarDestino(item.sistemaDestino)));
}

export function respostaContemDestinoForaDaSelecao<T extends ItemComSistemaDestino>(
  itens: T[],
  destinosSelecionados: string[],
  deveConsiderarItem: (item: T) => boolean = () => true,
) {
  if (destinosSelecionados.length === 0) {
    return false;
  }

  const destinos = new Set(destinosSelecionados.map(normalizarDestino));
  return itens.some((item) => (
    deveConsiderarItem(item)
    && !destinos.has(normalizarDestino(item.sistemaDestino))
  ));
}
