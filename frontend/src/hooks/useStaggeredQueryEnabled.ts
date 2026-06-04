import { useMemo, useSyncExternalStore } from 'react';

function criarStoreStaggered(enabled: boolean, delayMs: number) {
  const delay = Math.max(0, delayMs);
  const readyAt = Date.now() + delay;
  let snapshot = enabled && delay === 0;

  return {
    getSnapshot: () => snapshot,
    getServerSnapshot: () => enabled && delay === 0,
    subscribe: (notify: () => void) => {
      if (!enabled || snapshot) {
        return () => undefined;
      }

      const timeoutId = setTimeout(() => {
        snapshot = true;
        notify();
      }, Math.max(readyAt - Date.now(), 0));

      return () => clearTimeout(timeoutId);
    },
  };
}

export function useStaggeredQueryEnabled(enabled: boolean, delayMs: number): boolean {
  const store = useMemo(() => criarStoreStaggered(enabled, delayMs), [delayMs, enabled]);

  return useSyncExternalStore(store.subscribe, store.getSnapshot, store.getServerSnapshot);
}
