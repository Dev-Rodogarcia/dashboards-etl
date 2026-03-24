import { useQuery } from '@tanstack/react-query';
import { buscarManifestosGraficos, buscarManifestosOverview, buscarManifestosSerie, buscarManifestosTabela } from '../../api/endpoints/manifestosServico';
import type { ManifestosFiltro } from '../../types/manifestos';

const STALE_TIME = 5 * 60 * 1000;

export function useManifestosOverview(filtro: ManifestosFiltro) {
  return useQuery({
    queryKey: ['manifestos', 'overview', filtro],
    queryFn: () => buscarManifestosOverview(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosSerie(filtro: ManifestosFiltro) {
  return useQuery({
    queryKey: ['manifestos', 'serie', filtro],
    queryFn: () => buscarManifestosSerie(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosGraficos(filtro: ManifestosFiltro) {
  return useQuery({
    queryKey: ['manifestos', 'graficos', filtro],
    queryFn: () => buscarManifestosGraficos(filtro),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

export function useManifestosTabela(filtro: ManifestosFiltro, limite = 100) {
  return useQuery({
    queryKey: ['manifestos', 'tabela', filtro, limite],
    queryFn: () => buscarManifestosTabela(filtro, limite),
    staleTime: STALE_TIME,
    retry: 1,
  });
}
