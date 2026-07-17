import clienteAxios from '../clienteAxios';
import type {
  EslAtualizarColetaParams,
  EslCancelarColetaParams,
  EslCriarColetaParams,
  EslCriarCotacaoParams,
  EslColetaResposta,
  EslColetasListagemResposta,
  EslCotacaoResposta,
  EslDataIso,
  EslNotaFiscalValidada,
  EslValidarNfParams,
} from '../../types/esl';

const ESL_API_PATH = '/api/esl';

export async function validarNfEsl({ filial, chaveOrNumero }: EslValidarNfParams): Promise<EslNotaFiscalValidada> {
  const { data } = await clienteAxios.get<EslNotaFiscalValidada>(
    `${ESL_API_PATH}/coletas/validar-nf/${encodeURIComponent(chaveOrNumero)}`,
    { params: { filial } },
  );
  return data;
}

export async function listarColetasEsl(dataSolicitacao: EslDataIso, filial: string): Promise<EslColetasListagemResposta> {
  const { data } = await clienteAxios.get<EslColetasListagemResposta>(`${ESL_API_PATH}/coletas`, {
    params: { dataSolicitacao, filial },
  });
  return data;
}

export async function criarCotacaoEsl({ filial, solicitacao }: EslCriarCotacaoParams): Promise<EslCotacaoResposta> {
  const { data } = await clienteAxios.post<EslCotacaoResposta>(`${ESL_API_PATH}/cotacoes`, solicitacao, { params: { filial } });
  return data;
}

export async function criarColetaEsl({ filial, solicitacao }: EslCriarColetaParams): Promise<EslColetaResposta> {
  const { data } = await clienteAxios.post<EslColetaResposta>(`${ESL_API_PATH}/coletas`, solicitacao, { params: { filial } });
  return data;
}

export async function atualizarColetaEsl({
  filial,
  eslId,
  solicitacao,
}: EslAtualizarColetaParams): Promise<EslColetaResposta> {
  const { data } = await clienteAxios.patch<EslColetaResposta>(
    `${ESL_API_PATH}/coletas/${encodeURIComponent(eslId)}`,
    solicitacao,
    { params: { filial } },
  );
  return data;
}

export async function cancelarColetaEsl({
  filial,
  eslId,
  solicitacao,
}: EslCancelarColetaParams): Promise<EslColetaResposta> {
  const { data } = await clienteAxios.post<EslColetaResposta>(
    `${ESL_API_PATH}/coletas/${encodeURIComponent(eslId)}/cancelamento`,
    solicitacao,
    { params: { filial } },
  );
  return data;
}
