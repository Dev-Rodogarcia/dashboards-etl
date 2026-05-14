import type { TableApiFilters } from '../types/tableFilters';

export function aplicarFiltrosTabelaParams(params: URLSearchParams, filtrosTabela?: TableApiFilters) {
  if (!filtrosTabela) {
    return params;
  }

  for (const [chave, valor] of Object.entries(filtrosTabela)) {
    if (valor == null) {
      continue;
    }

    if (chave === 'tabelaColuna' && typeof valor === 'object' && !Array.isArray(valor)) {
      for (const [coluna, valorColuna] of Object.entries(valor)) {
        adicionarParametro(params, `f.tabelaColuna.${coluna}`, valorColuna);
      }
      continue;
    }

    adicionarParametro(params, `f.${chave}`, valor);
  }

  return params;
}

function adicionarParametro(params: URLSearchParams, chave: string, valor: unknown) {
  if (valor == null) {
    return;
  }

  if (Array.isArray(valor)) {
    valor
      .filter((item) => item != null && String(item).trim().length > 0)
      .forEach((item) => params.append(chave, String(item).trim()));
    return;
  }

  const texto = String(valor).trim();
  if (texto) {
    params.set(chave, texto);
  }
}
