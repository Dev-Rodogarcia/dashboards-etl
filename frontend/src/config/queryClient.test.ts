import { describe, expect, it } from 'vitest';
import { BASE_QUERY_STALE_TIME_MS, createDashboardQueryClient } from './queryClient';

describe('createDashboardQueryClient', () => {
  it('nao configura polling global para queries', () => {
    const queryClient = createDashboardQueryClient();
    const queryDefaults = queryClient.getDefaultOptions().queries;

    expect(queryDefaults).toMatchObject({
      staleTime: BASE_QUERY_STALE_TIME_MS,
      refetchOnWindowFocus: false,
    });
    expect(queryDefaults).not.toHaveProperty('refetchInterval');
    expect(queryDefaults).not.toHaveProperty('refetchIntervalInBackground');
  });
});
