import clienteAxios from '../clienteAxios';
import { extrairNomeArquivo, salvarBlobComoArquivo } from '../downloadArquivo';
import type { UserImportBatchRequest, UserImportPreviewResponse, UserImportResult } from '../../types/userImport';

const BASE = '/api/admin/acesso/usuarios/importacao';

export async function baixarTemplateImportacaoUsuarios(): Promise<void> {
  const response = await clienteAxios.get<Blob>(`${BASE}/template`, {
    responseType: 'blob',
  });
  const contentDisposition = response.headers['content-disposition'];
  const nomeArquivo = extrairNomeArquivo(
    Array.isArray(contentDisposition) ? contentDisposition[0] : contentDisposition,
    'usuarios-importacao-modelo.xlsx',
  );
  salvarBlobComoArquivo(response.data, nomeArquivo);
}

export async function preValidarImportacaoUsuarios(arquivo: File): Promise<UserImportPreviewResponse> {
  const formData = new FormData();
  formData.append('arquivo', arquivo);

  const { data } = await clienteAxios.post<UserImportPreviewResponse>(
    `${BASE}/pre-validacao`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  return data;
}

export async function revalidarImportacaoUsuarios(payload: UserImportBatchRequest): Promise<UserImportPreviewResponse> {
  const { data } = await clienteAxios.post<UserImportPreviewResponse>(`${BASE}/revalidacao`, payload);
  return data;
}

export async function importarUsuariosEmMassa(payload: UserImportBatchRequest): Promise<UserImportResult> {
  const { data } = await clienteAxios.post<UserImportResult>(BASE, payload);
  return data;
}
