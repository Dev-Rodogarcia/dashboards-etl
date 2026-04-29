import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  baixarTemplateImportacaoUsuarios,
  importarUsuariosEmMassa,
  preValidarImportacaoUsuarios,
  revalidarImportacaoUsuarios,
} from '../../api/endpoints/adminUsuariosImportacaoServico';
import type { UserImportBatchRequest } from '../../types/userImport';

export function useBaixarTemplateImportacaoUsuarios() {
  return useMutation({
    mutationFn: baixarTemplateImportacaoUsuarios,
  });
}

export function usePreValidarImportacaoUsuarios() {
  return useMutation({
    mutationFn: preValidarImportacaoUsuarios,
  });
}

export function useRevalidarImportacaoUsuarios() {
  return useMutation({
    mutationFn: (payload: UserImportBatchRequest) => revalidarImportacaoUsuarios(payload),
  });
}

export function useImportarUsuariosEmMassa() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UserImportBatchRequest) => importarUsuariosEmMassa(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'setores'] });
    },
  });
}
