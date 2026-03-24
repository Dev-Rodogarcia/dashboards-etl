import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  atribuirPapeis,
  atualizarSetor,
  atualizarUsuario,
  buscarOverrides,
  buscarPapeis,
  buscarCatalogoPermissoes,
  buscarSetores,
  buscarUsuariosAdmin,
  criarSetor,
  criarUsuario,
  excluirSetor,
  excluirUsuario,
  salvarOverrides,
} from '../../api/endpoints/adminAcessoServico';
import type { PermissaoOverride, SetorPayload, UsuarioPayload } from '../../types/access';

export function useCatalogoPermissoes() {
  return useQuery({
    queryKey: ['admin', 'acesso', 'catalogo'],
    queryFn: buscarCatalogoPermissoes,
    staleTime: 60 * 60 * 1000,
  });
}

export function useSetoresAdmin() {
  return useQuery({
    queryKey: ['admin', 'acesso', 'setores'],
    queryFn: buscarSetores,
  });
}

export function usePapeisAdmin() {
  return useQuery({
    queryKey: ['admin', 'acesso', 'papeis'],
    queryFn: buscarPapeis,
    staleTime: 60 * 60 * 1000,
  });
}

export function useUsuariosAdmin() {
  return useQuery({
    queryKey: ['admin', 'acesso', 'usuarios'],
    queryFn: buscarUsuariosAdmin,
  });
}

export function useUsuarioOverridesAdmin(usuarioId?: string | null) {
  return useQuery({
    queryKey: ['admin', 'acesso', 'usuarios', usuarioId, 'overrides'],
    queryFn: () => buscarOverrides(String(usuarioId)),
    enabled: Boolean(usuarioId),
  });
}

export function useCriarSetor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: SetorPayload) => criarSetor(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'setores'] });
    },
  });
}

export function useAtualizarSetor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: SetorPayload }) => atualizarSetor(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'setores'] });
    },
  });
}

export function useExcluirSetor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => excluirSetor(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'setores'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
    },
  });
}

export function useCriarUsuario() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UsuarioPayload) => criarUsuario(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
    },
  });
}

export function useAtualizarUsuario() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UsuarioPayload }) => atualizarUsuario(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
    },
  });
}

export function useExcluirUsuario() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => excluirUsuario(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
    },
  });
}

export function useAtribuirPapeisUsuario() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, papelIds }: { id: string; papelIds: number[] }) => atribuirPapeis(id, papelIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios', variables.id, 'overrides'] });
    },
  });
}

export function useSalvarOverridesUsuario() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, overrides }: { id: string; overrides: PermissaoOverride[] }) => salvarOverrides(id, overrides),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'acesso', 'usuarios', variables.id, 'overrides'] });
    },
  });
}
