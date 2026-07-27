import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  arquivarHomeSolicitacao,
  buscarHomeSolicitacoes,
  buscarMinhasHomeSolicitacoes,
  concluirHomeSolicitacao,
  criarHomeSolicitacao,
} from '../../api/endpoints/homeSolicitacoesServico';
import type { HomeRequestFormState } from '../../types/home';

export const HOME_SOLICITACOES_QUERY_KEY = ['home', 'solicitacoes'];
export const MINHAS_HOME_SOLICITACOES_QUERY_KEY = ['home', 'minhas-solicitacoes'];

export function useHomeSolicitacoes(enabled: boolean) {
  return useQuery({
    queryKey: HOME_SOLICITACOES_QUERY_KEY,
    queryFn: buscarHomeSolicitacoes,
    enabled,
  });
}

export function useMinhasHomeSolicitacoes() {
  return useQuery({
    queryKey: MINHAS_HOME_SOLICITACOES_QUERY_KEY,
    queryFn: buscarMinhasHomeSolicitacoes,
  });
}

export function useCriarHomeSolicitacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (form: HomeRequestFormState) => criarHomeSolicitacao(form),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: HOME_SOLICITACOES_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: MINHAS_HOME_SOLICITACOES_QUERY_KEY }),
    ]),
  });
}

export function useConcluirHomeSolicitacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: concluirHomeSolicitacao,
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: HOME_SOLICITACOES_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: MINHAS_HOME_SOLICITACOES_QUERY_KEY }),
    ]),
  });
}

export function useArquivarHomeSolicitacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: arquivarHomeSolicitacao,
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: HOME_SOLICITACOES_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: MINHAS_HOME_SOLICITACOES_QUERY_KEY }),
    ]),
  });
}
