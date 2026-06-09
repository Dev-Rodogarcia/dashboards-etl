export const POLLING_BASE_INTERVAL_MS = 30 * 60 * 1000;
export const POLLING_MAX_JITTER_MS = 60 * 1000;

export function calcularIntervaloComJitter(random = Math.random) {
  return POLLING_BASE_INTERVAL_MS + Math.floor(random() * POLLING_MAX_JITTER_MS);
}

export const OPERATIONAL_QUERY_POLLING_OPTIONS = {
  refetchInterval: () => calcularIntervaloComJitter(),
  refetchIntervalInBackground: true,
} as const;
