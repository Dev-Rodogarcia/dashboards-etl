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

export interface EtlLogExtracaoAuditoriaRow {
  id: number;
  entidade: string | null;
  timestampInicio: string | null;
  timestampFim: string | null;
  statusFinal: string | null;
  registrosExtraidos: number | null;
  paginasProcessadas: number | null;
  noopCount: number | null;
  mensagem: string | null;
}

export interface EtlTabelaAuditoriaResumoRow {
  tabelaAlvo: string;
  qtdExtracoes: number;
  qtdSucessos: number;
  qtdFalhas: number;
  totalRegistrosGravados: number;
  primeiraExtracao: string | null;
  ultimaExtracao: string | null;
  menorDataNegocio: string | null;
  maiorDataNegocio: string | null;
}

export interface EtlSaudeOverview {
  updatedAt: string;
  tempoMedioExecucaoSegundos: number;
  execucoesComErro: number;
  totalExecucoes: number;
  volumeProcessadoTotal: number;
  taxaSucesso: number;
}

export interface EtlTaxasDiariasPoint {
  dataReferencia: string;
  qtdSucesso: number;
  qtdFalha: number;
}

export interface EtlInsercoesAtualizacoesPoint {
  dataReferencia: string;
  insercoes: number;
  atualizacoes: number;
}

export interface EtlCategoriaErro {
  categoria: string;
  total: number;
}

export interface EtlSaudeCharts {
  categoriasErro: EtlCategoriaErro[];
}
