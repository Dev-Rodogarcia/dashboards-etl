export interface FiltroBase {
  dataInicio: string;
  dataFim: string;
}

export interface FiltroComDimensoes extends FiltroBase {
  filiais?: string[];
  status?: string[];
  clientes?: string[];
}

export type FiltroQuery = FiltroBase & Record<string, string | string[] | undefined>;

export interface KpiItem {
  label: string;
  valor: string | number;
  formato?: 'moeda' | 'numero' | 'porcentagem' | 'peso';
  trend?: {
    valor: number;
    direcao: 'up' | 'down' | 'neutral';
  };
}

export interface PaginacaoParams {
  pagina: number;
  tamanhoPagina: number;
  ordenarPor?: string;
  direcao?: 'asc' | 'desc';
}

export interface PaginacaoResponse<T> {
  conteudo: T[];
  totalElementos: number;
  totalPaginas: number;
  paginaAtual: number;
}
