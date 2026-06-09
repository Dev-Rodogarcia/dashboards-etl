import { QueryClient } from '@tanstack/react-query';

export const BASE_QUERY_STALE_TIME_MS = 5 * 60 * 1000;

export function createDashboardQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: BASE_QUERY_STALE_TIME_MS,
        refetchOnWindowFocus: false,
      },
    },
  });
}
