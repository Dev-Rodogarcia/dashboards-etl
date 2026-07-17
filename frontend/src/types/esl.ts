export type EslModal = 'rodo' | 'air';
export type EslTipoCalculo = 'price_table' | 'manual';
export type EslMotivoCancelamento = 'DESISTENCIA' | 'VOLUME_INCORRETO' | 'DUPLICIDADE' | 'OUTROS';

/** Datas e horários chegam do BFF em formato ISO. */
export type EslDataIso = string;
export type EslHorario = string;
export type EslDataHoraOffset = string;

export interface EslCidadeRequest {
  nome: string;
  uf: string;
}

export interface EslCotacaoTrechoRequest {
  modal: EslModal;
  tipoCalculo: EslTipoCalculo;
  tabelaPreco?: string;
  documentoPagador: string;
  documentoRemetente: string;
  documentoDestinatario: string;
  cidadeOrigem: EslCidadeRequest;
  cepOrigem?: string;
  cidadeDestino: EslCidadeRequest;
  cepDestino?: string;
  classificacaoProduto: string;
  valorNotasFiscais: number;
  quantidadeVolumes: number;
  pesoReal: number;
  volumeCubico: number;
}

export interface EslCriarCotacaoRequest {
  documentoCliente: string;
  validade: EslDataIso;
  referencia?: string;
  observacoes?: string;
  trechos: EslCotacaoTrechoRequest[];
}

export interface EslCotacaoTrechoResposta {
  valorFrete: number | null;
}

export interface EslCotacaoResposta {
  cotacaoId: string;
  numeroCotacao: number | null;
  referencia: string | null;
  validade: EslDataIso | null;
  urlImpressao: string | null;
  trechosPendentes: number | null;
  trechos: EslCotacaoTrechoResposta[];
  valorFreteTotal: number;
}

export interface EslColetaItemRequest {
  modal: EslModal;
  valorNotasFiscais: number;
  quantidadeVolumes: number;
  pesoRealNotasFiscais: number;
  documentoRemetente: string;
  documentoDestinatario: string;
  documentoPagador: string;
  invoiceIds: string[];
  altura?: number;
  comprimento?: number;
  largura?: number;
  pesoCubado?: number;
  previsaoEntrega?: EslDataHoraOffset;
}

export interface EslCriarColetaRequest {
  documentoCliente: string;
  referencia?: string;
  documentoLocalColeta: string;
  dataAgendada: EslDataIso;
  horaInicial: EslHorario;
  horaFinal: EslHorario;
  emailNotificacao?: string;
  telefoneNotificacao?: string;
  observacoes?: string;
  itens: EslColetaItemRequest[];
}

export interface EslAtualizarColetaRequest {
  dataSolicitacao?: EslDataIso;
  horaSolicitacao?: EslHorario;
  dataAgendada?: EslDataIso;
  horaInicial?: EslHorario;
  horaFinal?: EslHorario;
  emailNotificacao?: string;
  telefoneNotificacao?: string;
  observacoes?: string;
}

export interface EslCancelarColetaRequest {
  motivo: EslMotivoCancelamento;
}

export interface EslNotaFiscalValidada {
  invoiceId: string;
  chaveAcesso: string | null;
  numero: string | null;
  serie: string | null;
  dataEmissao: EslDataIso | null;
  status: string | null;
  valor: number | null;
  peso: number | null;
  volume: number | null;
}

export interface EslValidarNfParams {
  filial: string;
  chaveOrNumero: string;
}

export interface EslCriarCotacaoParams {
  filial: string;
  solicitacao: EslCriarCotacaoRequest;
}

export interface EslCriarColetaParams {
  filial: string;
  solicitacao: EslCriarColetaRequest;
}

export interface EslColetaResposta {
  coletaId: string;
  numeroColeta: number | null;
  status: string | null;
  dataSolicitacao: EslDataIso | null;
  horaSolicitacao: EslHorario | null;
  dataAgendada: EslDataIso | null;
  horaInicial: EslHorario | null;
  horaFinal: EslHorario | null;
  referencia: string | null;
  observacoes: string | null;
  motivoCancelamento: string | null;
  valorNotasFiscais: number | null;
  quantidadeVolumes: number | null;
  pesoNotasFiscais: number | null;
  pesoTaxado: number | null;
}

export interface EslColetaListagemItem {
  coletaId: string;
  numeroColeta: number | null;
  status: string | null;
  dataSolicitacao: EslDataIso | null;
  horaSolicitacao: EslHorario | null;
  dataAgendada: EslDataIso | null;
  horaInicial: EslHorario | null;
  horaFinal: EslHorario | null;
  referencia: string | null;
  motivoCancelamento: string | null;
  observacoes: string | null;
}

export interface EslColetasListagemResposta {
  itens: EslColetaListagemItem[];
  temProximaPagina: boolean;
  proximoCursor: string | null;
}

export type EslErroHttpStatus = 400 | 401 | 403 | 404 | 409 | 422 | 429 | 500 | 503 | 504;

export interface EslErroDetalhe {
  campo?: string;
  field?: string;
  path?: string;
  mensagem?: string;
  message?: string;
}

/** Contrato padronizado pelo ControllerAdvice do BFF; o payload GraphQL nunca chega ao React. */
export interface EslErroHttp {
  timestamp: string;
  status: EslErroHttpStatus;
  erro: string;
  mensagem: string;
  codigo?: 'ESL_OUTCOME_UNKNOWN' | string;
  erros?: Array<EslErroDetalhe | string>;
}

export interface EslAtualizarColetaParams {
  filial: string;
  eslId: string;
  solicitacao: EslAtualizarColetaRequest;
}

export interface EslCancelarColetaParams {
  filial: string;
  eslId: string;
  solicitacao: EslCancelarColetaRequest;
}
