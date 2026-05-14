import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { TableApiFilters, TableColumnFilterValue, TableFilters } from '../types/tableFilters';

const PARAMS = {
  busca: 't_busca',
  codigo: 't_codigo',
  placa: 't_placa',
  status: 't_status',
  razaoSocial: 't_razao_social',
  origem: 't_origem',
  destino: 't_destino',
} as const satisfies Record<Exclude<keyof TableFilters, 'columnFilters'>, string>;

const TABLE_PARAM_KEYS = Object.values(PARAMS);
const COLUMN_PARAM_PREFIX = 't_col_';
function limparTexto(valor: string | null): string | undefined {
  const texto = valor?.trim();
  return texto ? texto : undefined;
}

function lerFiltros(params: URLSearchParams): TableFilters {
  const columnFilters = lerFiltrosColuna(params);

  return {
    busca: limparTexto(params.get(PARAMS.busca)),
    codigo: limparTexto(params.get(PARAMS.codigo)),
    placa: limparTexto(params.get(PARAMS.placa)),
    status: params.getAll(PARAMS.status).filter((valor) => valor.trim().length > 0),
    razaoSocial: limparTexto(params.get(PARAMS.razaoSocial)),
    origem: limparTexto(params.get(PARAMS.origem)),
    destino: limparTexto(params.get(PARAMS.destino)),
    columnFilters: Object.keys(columnFilters).length > 0 ? columnFilters : undefined,
  };
}

function lerFiltrosColuna(params: URLSearchParams): Record<string, TableColumnFilterValue> {
  const chaves = Array.from(new Set(
    Array.from(params.keys())
      .filter((chave) => chave.startsWith(COLUMN_PARAM_PREFIX))
      .map((chave) => chave.slice(COLUMN_PARAM_PREFIX.length))
      .filter(Boolean),
  )).sort();

  return chaves.reduce<Record<string, TableColumnFilterValue>>((acc, chave) => {
    const valores = params
      .getAll(`${COLUMN_PARAM_PREFIX}${chave}`)
      .map((valor) => valor.trim())
      .filter(Boolean);

    if (valores.length === 1) {
      acc[chave] = valores[0];
    } else if (valores.length > 1) {
      acc[chave] = valores;
    }

    return acc;
  }, {});
}

function aplicarValor(params: URLSearchParams, campo: Exclude<keyof TableFilters, 'columnFilters'>, valor: string | string[] | undefined) {
  const chave = PARAMS[campo];
  params.delete(chave);

  if (Array.isArray(valor)) {
    valor
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => params.append(chave, item));
    return;
  }

  const texto = valor?.trim();
  if (texto) {
    params.set(chave, texto);
  }
}

function aplicarValorColuna(params: URLSearchParams, chaveColuna: string, valor: string | string[] | undefined) {
  const chave = `${COLUMN_PARAM_PREFIX}${chaveColuna}`;
  params.delete(chave);

  if (Array.isArray(valor)) {
    valor
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => params.append(chave, item));
    return;
  }

  const texto = valor?.trim();
  if (texto) {
    params.set(chave, texto);
  }
}

function toApiFilters(filtros: TableFilters): TableApiFilters {
  return {
    tabelaBusca: filtros.busca,
    tabelaCodigo: filtros.codigo,
    tabelaPlaca: filtros.placa,
    tabelaStatus: filtros.status && filtros.status.length > 0 ? filtros.status : undefined,
    tabelaRazaoSocial: filtros.razaoSocial,
    tabelaOrigem: filtros.origem,
    tabelaDestino: filtros.destino,
    tabelaColuna: filtros.columnFilters,
  };
}

function countColumnFilters(columnFilters: Record<string, TableColumnFilterValue> | undefined): number {
  return Object.values(columnFilters ?? {}).filter((valor) =>
    Array.isArray(valor) ? valor.length > 0 : Boolean(valor),
  ).length;
}

export function useAnalyticalTableFilters() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filtros = useMemo(() => lerFiltros(searchParams), [searchParams]);

  const apiFilters = useMemo(
    () => toApiFilters(filtros),
    [filtros],
  );

  const hiddenActiveCount = useMemo(
    () =>
      Number(Boolean(filtros.codigo))
      + Number(Boolean(filtros.placa))
      + Number((filtros.status?.length ?? 0) > 0)
      + Number(Boolean(filtros.razaoSocial))
      + Number(Boolean(filtros.origem))
      + Number(Boolean(filtros.destino))
      + countColumnFilters(filtros.columnFilters),
    [filtros],
  );

  const hasAnyFilter = hiddenActiveCount > 0 || Boolean(filtros.busca);
  const resetKey = useMemo(() => JSON.stringify(apiFilters), [apiFilters]);

  function setTextFilter(campo: Exclude<keyof TableFilters, 'status' | 'columnFilters'>, valor: string) {
    const next = new URLSearchParams(searchParams);
    aplicarValor(next, campo, valor);
    setSearchParams(next, { replace: true, preventScrollReset: true });
  }

  function setMultiFilter(campo: Extract<keyof TableFilters, 'status'>, valores: string[]) {
    const next = new URLSearchParams(searchParams);
    aplicarValor(next, campo, valores);
    setSearchParams(next, { replace: true, preventScrollReset: true });
  }

  function setColumnFilter(chaveColuna: string, valor: string | string[]) {
    const next = new URLSearchParams(searchParams);
    aplicarValorColuna(next, chaveColuna, valor);
    setSearchParams(next, { replace: true, preventScrollReset: true });
  }

  function clearTableFilters() {
    const next = new URLSearchParams(searchParams);
    TABLE_PARAM_KEYS.forEach((param) => next.delete(param));
    Array.from(next.keys())
      .filter((param) => param.startsWith(COLUMN_PARAM_PREFIX))
      .forEach((param) => next.delete(param));
    setSearchParams(next, { replace: true, preventScrollReset: true });
  }

  return {
    filters: filtros,
    apiFilters,
    hiddenActiveCount,
    hasAnyFilter,
    resetKey,
    setTextFilter,
    setMultiFilter,
    setColumnFilter,
    clearTableFilters,
  };
}
