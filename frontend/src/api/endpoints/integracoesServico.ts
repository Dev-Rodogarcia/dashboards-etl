import clienteAxios from '../clienteAxios';
import { aplicarFiltrosTabelaParams } from '../tableFilters';
import type { TableApiFilters } from '../../types/tableFilters';

export type IntegracoesEscopo = 'PENDENCIAS' | 'SUCESSO';

export interface IntegracaoMetricaConsolidada {
  sistemaDestino: string;
  totalRegistros: number;
  percentualXmlSucesso: number;
  percentualCanhotoSucesso: number;
}

export interface IntegracaoEvolucaoDiaria {
  data: string;
  total: number;
  sucessos: number;
  erros: number;
}

export interface ResumoTabelaIntegracao {
  entidadeTabela: string;
  totalProcessado: number;
  totalSucesso: number;
  totalErro: number;
  totalQuarentena: number;
}

export interface IntegracaoPendencia {
  id: number;
  sistemaDestino: string;
  occurrenceId: number | null;
  freightId: number | null;
  chaveNfe: string | null;
  numeroNf: number | null;
  serieNf: string | null;
  statusDados: string | null;
  statusCanhoto: string | null;
  mensagemErroDados: string | null;
  mensagemErroCanhoto: string | null;
  canhotoReferencia: string | null;
  canhotoMimeType: string | null;
  dataProcessamento: string | null;
  dataProcessamentoDados: string | null;
  dataProcessamentoCanhoto: string | null;
  possuiImagem?: boolean | null;
  possuiImagemCanhoto?: boolean | null;
  possuiImagemPayload?: boolean | null;
  imagemDisponivel?: boolean | null;
}

export interface IntegracoesPaginacao {
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeiraPagina: boolean;
  ultimaPagina: boolean;
}

export interface IntegracoesPendenciasPaginadas {
  itens: IntegracaoPendencia[];
  paginacao: IntegracoesPaginacao;
}

export interface IntegracoesAuditoriaResponse {
  geradoEm: string;
  metricasConsolidadas: IntegracaoMetricaConsolidada[];
  pendencias: IntegracoesPendenciasPaginadas;
}

export async function buscarIntegracoesAuditoria(
  pagina: number,
  tamanhoPagina: number,
  dataInicio: string,
  dataFim: string,
  filtrosTabela?: TableApiFilters,
  sortField?: keyof IntegracaoPendencia & string,
  sortDirection?: 'asc' | 'desc',
  escopo: IntegracoesEscopo = 'PENDENCIAS',
): Promise<IntegracoesAuditoriaResponse> {
  const params = new URLSearchParams();
  params.set('pagina', String(Math.max(0, pagina - 1)));
  params.set('tamanho', String(tamanhoPagina));
  params.set('escopo', escopo);
  params.set('dataInicial', dataInicio);
  params.set('dataFinal', dataFim);
  if (sortField) {
    params.set('sortField', sortField);
    params.set('sortDirection', sortDirection === 'desc' ? 'desc' : 'asc');
  }
  aplicarFiltrosTabelaParams(params, filtrosTabela);

  const { data } = await clienteAxios.get<IntegracoesAuditoriaResponse>('/api/painel/integracoes', { params });
  return data;
}

export async function buscarIntegracoesEvolucaoDiaria(
  dataInicio: string,
  dataFim: string,
  escopo?: IntegracoesEscopo,
): Promise<IntegracaoEvolucaoDiaria[]> {
  const params = new URLSearchParams();
  params.set('dataInicial', dataInicio);
  params.set('dataFinal', dataFim);
  if (escopo) {
    params.set('escopo', escopo);
  }

  const { data } = await clienteAxios.get<IntegracaoEvolucaoDiaria[]>(
    '/api/painel/integracoes/evolucao-diaria',
    { params },
  );
  return data;
}
