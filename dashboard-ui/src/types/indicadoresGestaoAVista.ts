export interface IndicadoresGestaoVistaFiltro {
  dataInicio: string;
  dataFim: string;
  filiais?: string[];
}

export interface PerformanceEntregaOverview {
  updatedAt: string;
  totalEntregas: number;
  entregasNoPrazo: number;
  entregasSemDados: number;
  pctNoPrazo: number;
}

export interface PerformanceEntregaSeriePoint {
  date: string | null;
  responsavelRegiaoDestino: string | null;
  totalEntregas: number;
  entregasNoPrazo: number;
  entregasSemDados: number;
  pctNoPrazo: number;
}

export interface PerformanceEntregaRow {
  numeroMinuta: number;
  dataFrete: string | null;
  filialEmissora: string | null;
  responsavelRegiaoDestino: string | null;
  previsaoEntrega: string | null;
  dataFinalizacao: string | null;
  performanceDiferencaDias: number | null;
  performanceStatus: string | null;
}

export interface UtilizacaoColetoresOverview {
  updatedAt: string;
  ordensConferencia: number;
  manifestosEmitidos: number;
  manifestosDescarregamento: number;
  totalManifestos: number;
  pctUtilizacao: number;
}

export interface UtilizacaoColetoresSeriePoint {
  date: string | null;
  filial: string | null;
  ordensConferencia: number;
  manifestosEmitidos: number;
  manifestosDescarregamento: number;
  totalManifestos: number;
  pctUtilizacao: number;
}

export interface UtilizacaoColetoresRow {
  chave: string;
  date: string | null;
  filial: string | null;
  ordensConferencia: number;
  manifestosEmitidos: number;
  manifestosDescarregamento: number;
  totalManifestos: number;
  pctUtilizacao: number;
}

export interface CubagemMercadoriasOverview {
  updatedAt: string;
  totalFretes: number;
  fretesCubados: number;
  fretesComPesoReal: number;
  pctCubagem: number;
}

export interface CubagemMercadoriasSeriePoint {
  date: string | null;
  filial: string | null;
  totalFretes: number;
  fretesCubados: number;
  pctCubagem: number;
}

export interface CubagemMercadoriasRow {
  numeroMinuta: number;
  dataFrete: string | null;
  filialEmissora: string | null;
  pagador: string | null;
  destino: string | null;
  pesoTaxado: number;
  pesoReal: number;
  pesoCubado: number;
  totalM3: number;
  cubado: boolean;
}

export interface IndenizacaoMercadoriasOverview {
  updatedAt: string;
  totalSinistros: number;
  valorIndenizadoAbs: number;
  valorIndenizadoOriginal: number;
  faturamentoBase: number;
  pctIndenizacao: number;
}

export interface IndenizacaoMercadoriasSeriePoint {
  date: string | null;
  filial: string | null;
  totalSinistros: number;
  valorIndenizadoAbs: number;
  faturamentoBase: number;
  pctIndenizacao: number;
}

export interface IndenizacaoMercadoriasRow {
  numeroSinistro: number;
  dataAbertura: string | null;
  filial: string | null;
  minuta: number | null;
  resultadoFinalOriginal: number;
  resultadoFinalAbs: number;
  ocorrencia: string | null;
  solucao: string | null;
  pctSobreFaturamentoFilial: number;
}

export interface HorariosCorteOverview {
  updatedAt: string;
  saidasNoHorario: number;
  totalProgramado: number;
  pctNoHorario: number;
  ultimaImportacaoEm: string | null;
  ultimaImportacaoArquivo: string | null;
}

export interface HorariosCorteSeriePoint {
  date: string | null;
  filial: string | null;
  saidasNoHorario: number;
  totalProgramado: number;
  pctNoHorario: number;
}

export interface HorarioCorteRow {
  id: number;
  data: string | null;
  filial: string | null;
  linhaOuOperacao: string | null;
  inicio: string | null;
  manifestado: string | null;
  smGerada: string | null;
  corte: string | null;
  saidaEfetiva: string | null;
  horarioCorte: string | null;
  saiuNoHorario: boolean | null;
  atrasoMinutos: number | null;
  observacao: string | null;
  nomeArquivo: string | null;
  importadoEm: string | null;
  importadoPor: string | null;
}

export interface HorariosCorteImportacaoMensagem {
  linha: number;
  linhaOuOperacao: string | null;
  mensagem: string;
}

export interface HorariosCorteImportacaoResultado {
  arquivo: string;
  importadoEm: string;
  linhasProcessadas: number;
  linhasImportadas: number;
  linhasSubstituidas: number;
  linhasIgnoradas: number;
  avisos: HorariosCorteImportacaoMensagem[];
  rejeicoes: HorariosCorteImportacaoMensagem[];
}
