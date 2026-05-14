export type TableColumnFilterValue = string | string[];

export interface TableFilters {
  busca?: string;
  codigo?: string;
  placa?: string;
  status?: string[];
  razaoSocial?: string;
  origem?: string;
  destino?: string;
  columnFilters?: Record<string, TableColumnFilterValue>;
}

export interface TableApiFilters {
  tabelaBusca?: string;
  tabelaCodigo?: string;
  tabelaPlaca?: string;
  tabelaStatus?: string[];
  tabelaRazaoSocial?: string;
  tabelaOrigem?: string;
  tabelaDestino?: string;
  tabelaColuna?: Record<string, TableColumnFilterValue>;
}

export type TableFilterField = Exclude<keyof TableFilters, 'busca' | 'columnFilters'>;
