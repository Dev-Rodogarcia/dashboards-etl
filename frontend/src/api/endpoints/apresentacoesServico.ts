import clienteAxios from '../clienteAxios';
import type { ApresentacaoSequencia, ApresentacaoSequenciaPayload } from '../../types/apresentacao';

const BASE = '/api/apresentacoes';

export async function listarApresentacoes(): Promise<ApresentacaoSequencia[]> {
  const { data } = await clienteAxios.get<ApresentacaoSequencia[]>(BASE);
  return data;
}

export async function criarApresentacao(payload: ApresentacaoSequenciaPayload): Promise<ApresentacaoSequencia> {
  const { data } = await clienteAxios.post<ApresentacaoSequencia>(BASE, payload);
  return data;
}

export async function excluirApresentacao(id: number): Promise<void> {
  await clienteAxios.delete(`${BASE}/${id}`);
}
