import { isAxiosError } from 'axios';
import clienteAxios from '../clienteAxios';
import { aplicarFiltrosTabelaParams } from '../tableFilters';
import type { TableApiFilters } from '../../types/tableFilters';

const PREFIXO_IMAGEM_BASE64 = /^data:image\/[a-z0-9.+-]+;base64,/i;
const CAMPOS_IMAGEM_CANHOTO = [
  'imagemBase64',
  'imagem',
  'imageBase64',
  'canhotoBase64',
  'conteudoBase64',
  'foto',
] as const;

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
  possuiImagem?: boolean | null;
  possuiImagemCanhoto?: boolean | null;
  possuiImagemPayload?: boolean | null;
  imagemDisponivel?: boolean | null;
}

type ImagemCanhotoPayload = Partial<Record<(typeof CAMPOS_IMAGEM_CANHOTO)[number], unknown>> & {
  mime?: unknown;
  mimeType?: unknown;
  contentType?: unknown;
};

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
  sortField?: keyof IntegracaoPendencia & string,
  sortDirection?: 'asc' | 'desc',
): Promise<IntegracoesAuditoriaResponse> {
  const params = new URLSearchParams();
  params.set('pagina', String(Math.max(0, pagina - 1)));
  params.set('tamanho', String(tamanhoPagina));
  if (sortField) {
    params.set('sortField', sortField);
    params.set('sortDirection', sortDirection === 'desc' ? 'desc' : 'asc');
  }
  aplicarFiltrosTabelaParams(params, filtrosTabela);

  const { data } = await clienteAxios.get<IntegracoesAuditoriaResponse>('/api/painel/integracoes', { params });
  return data;
}

function extrairImagemCanhoto(data: unknown): { valor: string | null; mimeType?: string } {
  if (typeof data === 'string') {
    return { valor: data };
  }

  if (!data || typeof data !== 'object') {
    return { valor: null };
  }

  const payload = data as ImagemCanhotoPayload;
  const valor = CAMPOS_IMAGEM_CANHOTO
    .map((campo) => payload[campo])
    .find((item): item is string => typeof item === 'string' && item.trim().length > 0);
  const mimeType = typeof payload.mime === 'string'
    ? payload.mime
    : typeof payload.mimeType === 'string'
    ? payload.mimeType
    : typeof payload.contentType === 'string'
    ? payload.contentType
    : undefined;

  return { valor: valor ?? null, mimeType };
}

export function normalizarImagemCanhotoSrc(data: unknown): string | null {
  const { valor, mimeType } = extrairImagemCanhoto(data);
  const imagem = valor?.trim();

  if (!imagem || imagem.toLowerCase() === 'null') {
    return null;
  }

  if (PREFIXO_IMAGEM_BASE64.test(imagem)) {
    return imagem;
  }

  const tipoImagem = normalizarMimeType(mimeType);
  return `data:${tipoImagem};base64,${imagem}`;
}

function normalizarMimeType(mimeType: string | undefined): string {
  const tipo = mimeType?.trim();
  if (!tipo) {
    return 'image/jpeg';
  }

  if (tipo.startsWith('data:')) {
    return tipo
      .replace(/^data:/i, '')
      .replace(/;base64$/i, '');
  }

  return tipo.replace(/;base64$/i, '');
}

export async function buscarImagemCanhotoIntegracao(id: number): Promise<string | null> {
  try {
    const { data } = await clienteAxios.get<unknown>(`/api/painel/integracoes/logs/${id}/imagem`, {
      headers: { Accept: 'application/json, text/plain, */*' },
    });
    return normalizarImagemCanhotoSrc(data);
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null;
    }
    throw error;
  }
}
