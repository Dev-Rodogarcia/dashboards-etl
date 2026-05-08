import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  arquivarHomeComunicado,
  atualizarHomeComunicado,
  buscarHomeComunicados,
  criarHomeComunicado,
} from '../../api/endpoints/homeComunicadosServico';
import { HOME_COMUNICADOS_API_ENABLED } from '../../config/api';
import type { HomeNoticePayload } from '../../types/home';

const HOME_COMUNICADOS_QUERY_KEY = ['home', 'comunicados'];

export function useHomeComunicados() {
  return useQuery({
    queryKey: HOME_COMUNICADOS_QUERY_KEY,
    queryFn: buscarHomeComunicados,
    enabled: HOME_COMUNICADOS_API_ENABLED,
    staleTime: 60 * 1000,
    retry: false,
  });
}

export function useCriarHomeComunicado() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: HomeNoticePayload) => criarHomeComunicado(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: HOME_COMUNICADOS_QUERY_KEY });
    },
  });
}

export function useAtualizarHomeComunicado() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: HomeNoticePayload }) =>
      atualizarHomeComunicado(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: HOME_COMUNICADOS_QUERY_KEY });
    },
  });
}

export function useArquivarHomeComunicado() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => arquivarHomeComunicado(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: HOME_COMUNICADOS_QUERY_KEY });
    },
  });
}
