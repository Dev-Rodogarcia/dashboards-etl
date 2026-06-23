import clienteAxios from '../clienteAxios';
import { aplicarFiltrosTabelaParams } from '../tableFilters';
import type { TableApiFilters } from '../../types/tableFilters';

export interface IntegracaoMetricaConsolidada {
  sistemaDestino: string;
  totalRegistros: number;
  percentualXmlSucesso: number;
  percentualCanhotoSucesso: number;
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
  dataProcessamento: string | null;
  dataProcessamentoDados: string | null;
  dataProcessamentoCanhoto: string | null;
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
  filtrosTabela?: TableApiFilters,
): Promise<IntegracoesAuditoriaResponse> {
  const params = new URLSearchParams();
  params.set('pagina', String(Math.max(0, pagina - 1)));
  params.set('tamanho', String(tamanhoPagina));
  aplicarFiltrosTabelaParams(params, filtrosTabela);

  const { data } = await clienteAxios.get<IntegracoesAuditoriaResponse>('/api/painel/integracoes', { params });
  return data;
}
