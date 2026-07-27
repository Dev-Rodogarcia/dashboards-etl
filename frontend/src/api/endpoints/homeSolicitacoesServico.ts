import clienteAxios from '../clienteAxios';
import type { HomeRequest, HomeRequestApi, HomeRequestAttachment, HomeRequestFormState, HomeRequestPayload } from '../../types/home';

const HOME_SOLICITACOES_ENDPOINT = '/api/painel/home/solicitacoes';

export function requestToPayload(form: HomeRequestFormState): HomeRequestPayload {
  return {
    tipo: form.type,
    titulo: form.title.trim(),
    descricao: form.description.trim(),
    resultadoEsperado: form.expectedResult.trim(),
    localAplicacao: form.applicationLocation.trim(),
  };
}

export function mapRequestFromApi(request: HomeRequestApi): HomeRequest {
  return {
    ...request,
    expectedResult: request.resultadoEsperado ?? '',
    applicationLocation: request.localAplicacao ?? '',
    completedAt: request.concluidoEm,
    attachments: (request.anexos ?? []).map((attachment) => ({ ...attachment, id: String(attachment.id) })),
  };
}

export async function criarHomeSolicitacao(form: HomeRequestFormState): Promise<HomeRequest> {
  const dados = new FormData();
  dados.append('solicitacao', new Blob([JSON.stringify(requestToPayload(form))], { type: 'application/json' }));
  form.attachments.forEach((attachment) => dados.append('anexos', attachment));

  const { data } = await clienteAxios.post<HomeRequestApi>(HOME_SOLICITACOES_ENDPOINT, dados);
  return mapRequestFromApi(data);
}

export async function buscarHomeSolicitacoes(): Promise<HomeRequest[]> {
    const { data } = await clienteAxios.get<HomeRequestApi[]>(HOME_SOLICITACOES_ENDPOINT);
    return data.map(mapRequestFromApi);
}

export async function buscarMinhasHomeSolicitacoes(): Promise<HomeRequest[]> {
  const { data } = await clienteAxios.get<HomeRequestApi[]>(`${HOME_SOLICITACOES_ENDPOINT}/minhas`);
  return data.map(mapRequestFromApi);
}

export async function concluirHomeSolicitacao(id: string): Promise<HomeRequest> {
  const { data } = await clienteAxios.patch<HomeRequestApi>(`${HOME_SOLICITACOES_ENDPOINT}/${id}/concluir`);
  return mapRequestFromApi(data);
}

export async function arquivarHomeSolicitacao(id: string): Promise<void> {
  await clienteAxios.delete(`${HOME_SOLICITACOES_ENDPOINT}/${id}`);
}

export async function baixarHomeSolicitacaoAnexo(
  requestId: string,
  attachment: HomeRequestAttachment,
): Promise<void> {
  const response = await clienteAxios.get<Blob>(
    `${HOME_SOLICITACOES_ENDPOINT}/${requestId}/anexos/${attachment.id}`,
    { responseType: 'blob' },
  );
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = attachment.nomeOriginal;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
