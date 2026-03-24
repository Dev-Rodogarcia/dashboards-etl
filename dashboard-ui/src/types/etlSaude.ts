export interface EtlExecucaoRow {
  id: number;
  inicio: string | null;
  fim: string | null;
  duracaoSegundos: number | null;
  data: string | null;
  status: string | null;
  totalRegistros: number | null;
  categoriaErro: string | null;
  mensagemErro: string | null;
}

export interface EtlSaudeOverview {
  updatedAt: string;
  tempoMedioExecucaoSegundos: number;
  execucoesComErro: number;
  totalExecucoes: number;
  volumeProcessadoTotal: number;
  taxaSucesso: number;
}

export interface EtlExecucaoTrendPoint {
  date: string;
  execucoes: number;
  erros: number;
  volumeProcessado: number;
  duracaoMedia: number;
}

export interface EtlCategoriaErro {
  categoria: string;
  total: number;
}

export interface EtlSaudeCharts {
  categoriasErro: EtlCategoriaErro[];
}
