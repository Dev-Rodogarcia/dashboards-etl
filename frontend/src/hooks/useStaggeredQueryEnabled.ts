import { useEffect, useState } from 'react';

export function useStaggeredQueryEnabled(enabled: boolean, delayMs: number): boolean {
  const [staggeredEnabled, setStaggeredEnabled] = useState(() => enabled && delayMs <= 0);

  useEffect(() => {
    if (!enabled) {
      setStaggeredEnabled(false);
      return undefined;
    }

    if (delayMs <= 0) {
      setStaggeredEnabled(true);
      return undefined;
    }

    const timeoutId = setTimeout(() => setStaggeredEnabled(true), delayMs);
    return () => clearTimeout(timeoutId);
  }, [delayMs, enabled]);

  return staggeredEnabled;
}
