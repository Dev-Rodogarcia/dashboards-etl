import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  arquivarHomeSolicitacao,
  buscarHomeSolicitacoes,
  concluirHomeSolicitacao,
  criarHomeSolicitacao,
} from '../../api/endpoints/homeSolicitacoesServico';
import type { HomeRequestPayload } from '../../types/home';

export const HOME_SOLICITACOES_QUERY_KEY = ['home', 'solicitacoes'];

export function useHomeSolicitacoes(enabled: boolean) {
  return useQuery({
    queryKey: HOME_SOLICITACOES_QUERY_KEY,
    queryFn: buscarHomeSolicitacoes,
    enabled,
  });
}

export function useCriarHomeSolicitacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: HomeRequestPayload) => criarHomeSolicitacao(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: HOME_SOLICITACOES_QUERY_KEY }),
  });
}

export function useConcluirHomeSolicitacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: concluirHomeSolicitacao,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: HOME_SOLICITACOES_QUERY_KEY }),
  });
}

export function useArquivarHomeSolicitacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: arquivarHomeSolicitacao,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: HOME_SOLICITACOES_QUERY_KEY }),
  });
}
