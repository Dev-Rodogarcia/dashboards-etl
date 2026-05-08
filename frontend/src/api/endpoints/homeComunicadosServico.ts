import clienteAxios from '../clienteAxios';
import type { HomeNotice, HomeNoticeApi, HomeNoticeFormState, HomeNoticePayload } from '../../types/home';

const HOME_COMUNICADOS_ENDPOINT = '/api/painel/home/comunicados';

export function formatNoticeDate(value?: string | null): string {
  if (!value) return 'Atualização recente';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Atualização recente';

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

export function mapNoticeFromApi(notice: HomeNoticeApi): HomeNotice {
  return {
    id: notice.id,
    title: notice.titulo,
    body: notice.corpo,
    tag: notice.tag,
    audience: notice.publicoAlvo,
    date: formatNoticeDate(notice.publicadoEm),
    publishedAt: notice.publicadoEm,
    updatedBy: notice.atualizadoPor,
  };
}

export function noticeToPayload(form: HomeNoticeFormState): HomeNoticePayload {
  return {
    titulo: form.title.trim(),
    corpo: form.body.trim(),
    tag: form.tag,
    publicoAlvo: form.audience.trim() || 'Todos',
  };
}

export function noticeToForm(notice: HomeNotice): HomeNoticeFormState {
  return {
    title: notice.title,
    body: notice.body,
    tag: notice.tag,
    audience: notice.audience,
  };
}

export async function buscarHomeComunicados(): Promise<HomeNotice[]> {
  const { data } = await clienteAxios.get<HomeNoticeApi[]>(HOME_COMUNICADOS_ENDPOINT);
  return data.map(mapNoticeFromApi);
}

export async function criarHomeComunicado(payload: HomeNoticePayload): Promise<HomeNotice> {
  const { data } = await clienteAxios.post<HomeNoticeApi>(HOME_COMUNICADOS_ENDPOINT, payload);
  return mapNoticeFromApi(data);
}

export async function atualizarHomeComunicado(id: string, payload: HomeNoticePayload): Promise<HomeNotice> {
  const { data } = await clienteAxios.put<HomeNoticeApi>(`${HOME_COMUNICADOS_ENDPOINT}/${id}`, payload);
  return mapNoticeFromApi(data);
}

export async function arquivarHomeComunicado(id: string): Promise<void> {
  await clienteAxios.delete(`${HOME_COMUNICADOS_ENDPOINT}/${id}`);
}
