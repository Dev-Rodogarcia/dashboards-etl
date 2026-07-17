import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  atualizarColetaEsl,
  cancelarColetaEsl,
  criarColetaEsl,
  criarCotacaoEsl,
  listarColetasEsl,
  validarNfEsl,
} from '../../api/endpoints/eslService';
import type {
  EslAtualizarColetaParams,
  EslCancelarColetaParams,
  EslCriarColetaParams,
  EslCriarCotacaoParams,
  EslDataIso,
  EslValidarNfParams,
} from '../../types/esl';

/**
 * Operações ESL são síncronas e transacionais no sistema remoto. Nenhuma mutation
 * pode ser repetida automaticamente, inclusive após 500/504, pois o resultado
 * remoto pode ser desconhecido.
 */
const ESL_MUTATION_OPTIONS = { retry: false } as const;
export const ESL_COLETAS_DIARIAS_QUERY_KEY = ['esl', 'coletas', 'diario'] as const;

export function eslColetasDiariasQueryKey(dataSolicitacao: EslDataIso, filial: string) {
  return [...ESL_COLETAS_DIARIAS_QUERY_KEY, dataSolicitacao, filial] as const;
}

export function useEslColetasDiarias(dataSolicitacao: EslDataIso, filial: string, enabled = true) {
  return useQuery({
    queryKey: eslColetasDiariasQueryKey(dataSolicitacao, filial),
    queryFn: () => listarColetasEsl(dataSolicitacao, filial),
    enabled: enabled && Boolean(dataSolicitacao) && Boolean(filial),
    staleTime: 0,
    retry: false,
    refetchOnWindowFocus: false,
  });
}

export function useValidarNfEsl() {
  return useMutation({
    mutationFn: (parametros: EslValidarNfParams) => validarNfEsl(parametros),
    ...ESL_MUTATION_OPTIONS,
  });
}

export function useCriarCotacaoEsl() {
  return useMutation({
    mutationFn: (parametros: EslCriarCotacaoParams) => criarCotacaoEsl(parametros),
    ...ESL_MUTATION_OPTIONS,
  });
}

export function useCriarColetaEsl() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (parametros: EslCriarColetaParams) => criarColetaEsl(parametros),
    ...ESL_MUTATION_OPTIONS,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ESL_COLETAS_DIARIAS_QUERY_KEY });
    },
  });
}

export function useAtualizarColetaEsl() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (parametros: EslAtualizarColetaParams) => atualizarColetaEsl(parametros),
    ...ESL_MUTATION_OPTIONS,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ESL_COLETAS_DIARIAS_QUERY_KEY });
    },
  });
}

export function useCancelarColetaEsl() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (parametros: EslCancelarColetaParams) => cancelarColetaEsl(parametros),
    ...ESL_MUTATION_OPTIONS,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ESL_COLETAS_DIARIAS_QUERY_KEY });
    },
  });
}
