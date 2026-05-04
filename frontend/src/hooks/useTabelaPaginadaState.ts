import { useState } from 'react';

interface TabelaPaginadaState {
  resetKey: string;
  pagina: number;
  tamanhoPagina: number;
}

export function useTabelaPaginadaState(resetKey: string) {
  const [state, setState] = useState<TabelaPaginadaState>({
    resetKey,
    pagina: 1,
    tamanhoPagina: 10,
  });

  const pagina = state.resetKey === resetKey ? state.pagina : 1;

  function alterarTamanhoPagina(proximoTamanho: number) {
    setState((atual) => ({
      resetKey,
      pagina: atual.resetKey === resetKey ? atual.pagina : 1,
      tamanhoPagina: proximoTamanho,
    }));
  }

  function alterarPagina(proximaPagina: number) {
    setState((atual) => ({
      resetKey,
      pagina: proximaPagina,
      tamanhoPagina: atual.tamanhoPagina,
    }));
  }

  return {
    pagina,
    tamanhoPagina: state.tamanhoPagina,
    setPagina: alterarPagina,
    setTamanhoPagina: alterarTamanhoPagina,
  };
}
