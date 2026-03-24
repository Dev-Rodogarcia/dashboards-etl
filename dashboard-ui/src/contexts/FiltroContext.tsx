/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useMemo } from 'react';
import type { ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';

interface FiltroContexto {
  dataInicio: string;
  dataFim: string;
  filtros: Record<string, string[]>;
  setDataInicio: (data: string) => void;
  setDataFim: (data: string) => void;
  setFiltro: (chave: string, valores: string[]) => void;
  limparFiltros: () => void;
}

const FiltroContext = createContext<FiltroContexto | null>(null);
const PREFIXO_FILTRO = 'f.';

function dataHoje(): string {
  return new Date().toISOString().slice(0, 10);
}

function data30DiasAtras(): string {
  const d = new Date();
  d.setDate(d.getDate() - 30);
  return d.toISOString().slice(0, 10);
}

function lerFiltros(params: URLSearchParams): Record<string, string[]> {
  const filtros: Record<string, string[]> = {};

  for (const [chave, valor] of params.entries()) {
    if (!chave.startsWith(PREFIXO_FILTRO) || !valor) {
      continue;
    }

    const nomeFiltro = chave.slice(PREFIXO_FILTRO.length);
    if (!nomeFiltro) {
      continue;
    }

    if (!filtros[nomeFiltro]) {
      filtros[nomeFiltro] = [];
    }

    filtros[nomeFiltro].push(valor);
  }

  return filtros;
}

export function FiltroProvider({ children }: { children: ReactNode }) {
  const [searchParams, setSearchParams] = useSearchParams();

  const dataInicio = searchParams.get('dataInicio') ?? data30DiasAtras();
  const dataFim = searchParams.get('dataFim') ?? dataHoje();
  const filtros = useMemo(() => lerFiltros(searchParams), [searchParams]);

  const atualizarParams = useCallback(
    (mutator: (params: URLSearchParams) => void) => {
      const next = new URLSearchParams(searchParams);
      mutator(next);
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams]
  );

  const setDataInicio = useCallback(
    (data: string) => {
      atualizarParams((params) => {
        params.set('dataInicio', data);
      });
    },
    [atualizarParams]
  );

  const setDataFim = useCallback(
    (data: string) => {
      atualizarParams((params) => {
        params.set('dataFim', data);
      });
    },
    [atualizarParams]
  );

  const setFiltro = useCallback(
    (chave: string, valores: string[]) => {
      atualizarParams((params) => {
        params.delete(`${PREFIXO_FILTRO}${chave}`);

        valores
          .filter((valor) => valor.trim().length > 0)
          .forEach((valor) => params.append(`${PREFIXO_FILTRO}${chave}`, valor));
      });
    },
    [atualizarParams]
  );

  const limparFiltros = useCallback(() => {
    setSearchParams(
      {
        dataInicio: data30DiasAtras(),
        dataFim: dataHoje(),
      },
      { replace: true }
    );
  }, [setSearchParams]);

  return (
    <FiltroContext.Provider
      value={{
        dataInicio,
        dataFim,
        filtros,
        setDataInicio,
        setDataFim,
        setFiltro,
        limparFiltros,
      }}
    >
      {children}
    </FiltroContext.Provider>
  );
}

export function useFiltro(): FiltroContexto {
  const ctx = useContext(FiltroContext);
  if (!ctx) throw new Error('useFiltro deve ser usado dentro de FiltroProvider');
  return ctx;
}
