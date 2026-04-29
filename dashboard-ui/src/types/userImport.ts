import type { SetorPayload } from './access';

export type UserImportPreviewStatus =
  | 'PRONTA'
  | 'ERRO_VALIDACAO'
  | 'CONFLITO_EMAIL_EXISTENTE'
  | 'SETOR_INEXISTENTE';

export interface UserImportTotals {
  totalLinhas: number;
  validas: number;
  invalidas: number;
}

export interface UserImportPreviewRow {
  linha: number;
  nome: string | null;
  email: string | null;
  setorOriginal: string | null;
  setorResolvido: string | null;
  status: UserImportPreviewStatus;
  mensagens: string[];
}

export interface UserImportPreviewResponse {
  importacaoId: string;
  arquivo: string;
  totais: UserImportTotals;
  setoresInexistentes: string[];
  podeImportar: boolean;
  linhasPreview: UserImportPreviewRow[];
}

export interface UserImportSectorResolution {
  setorOriginal: string;
  setorDestinoId: string;
}

export interface UserImportBatchRequest {
  importacaoId: string;
  resolucoesSetor: UserImportSectorResolution[];
}

export interface UserImportCreated {
  nome: string;
  email: string;
  setor: string;
}

export interface UserImportIgnored {
  email: string;
  motivo: string;
}

export interface UserImportError {
  linha: number;
  email: string | null;
  motivo: string;
  tipoErro: string;
}

export interface UserImportCredential {
  email: string;
  senhaProvisoria: string;
}

export interface UserImportResult {
  totalProcessados: number;
  totalCriados: number;
  totalIgnorados: number;
  totalErros: number;
  listaCriados: UserImportCreated[];
  listaIgnorados: UserImportIgnored[];
  listaErros: UserImportError[];
  credenciaisTemporarias: UserImportCredential[];
}

export interface UserImportLocalFileState {
  arquivo: File;
  extensaoValida: boolean;
}

export interface CreateSetorImportDraft extends SetorPayload {
  nomeOriginalImportacao: string;
}
