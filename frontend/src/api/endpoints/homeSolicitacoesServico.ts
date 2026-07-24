import clienteAxios from '../clienteAxios';
import type { HomeRequest, HomeRequestApi, HomeRequestFormState, HomeRequestPayload } from '../../types/home';

const HOME_SOLICITACOES_ENDPOINT = '/api/painel/home/solicitacoes';

export function requestToPayload(form: HomeRequestFormState): HomeRequestPayload {
  return {
    tipo: form.type,
    titulo: form.title.trim(),
    descricao: form.description.trim(),
    resultadoEsperado: form.expectedResult.trim(),
  };
}

export function mapRequestFromApi(request: HomeRequestApi): HomeRequest {
  return {
    ...request,
    expectedResult: request.resultadoEsperado ?? '',
    completedAt: request.concluidoEm,
  };
}

export async function criarHomeSolicitacao(payload: HomeRequestPayload): Promise<HomeRequest> {
  const { data } = await clienteAxios.post<HomeRequestApi>(HOME_SOLICITACOES_ENDPOINT, payload);
  return mapRequestFromApi(data);
}

export async function buscarHomeSolicitacoes(): Promise<HomeRequest[]> {
  const { data } = await clienteAxios.get<HomeRequestApi[]>(HOME_SOLICITACOES_ENDPOINT);
  return data.map(mapRequestFromApi);
}

export async function concluirHomeSolicitacao(id: string): Promise<HomeRequest> {
  const { data } = await clienteAxios.patch<HomeRequestApi>(`${HOME_SOLICITACOES_ENDPOINT}/${id}/concluir`);
  return mapRequestFromApi(data);
}

export async function arquivarHomeSolicitacao(id: string): Promise<void> {
  await clienteAxios.delete(`${HOME_SOLICITACOES_ENDPOINT}/${id}`);
}
