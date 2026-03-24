export function montarQueryParams<T extends { dataInicio: string; dataFim: string }>(
  filtro: T
): URLSearchParams {
  const params = new URLSearchParams();

  params.set('dataInicio', filtro.dataInicio);
  params.set('dataFim', filtro.dataFim);

  for (const [chave, valor] of Object.entries(filtro)) {
    if (chave === 'dataInicio' || chave === 'dataFim' || valor == null) {
      continue;
    }

    if (Array.isArray(valor)) {
      valor
        .filter((item) => item != null && item.length > 0)
        .forEach((item) => params.append(`f.${chave}`, item));
      continue;
    }

    params.set(`f.${chave}`, String(valor));
  }

  return params;
}
