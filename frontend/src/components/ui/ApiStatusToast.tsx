import { useEffect, useState } from 'react';
import { Clock, WifiOff, X } from 'lucide-react';
import { API_STATUS_ALERT_EVENT, type ApiStatusAlertDetail } from '../../api/clienteAxios';

export default function ApiStatusToast() {
  const [alerta, setAlerta] = useState<ApiStatusAlertDetail | null>(null);

  useEffect(() => {
    function handleApiStatusAlert(event: Event) {
      setAlerta((event as CustomEvent<ApiStatusAlertDetail>).detail);
    }

    window.addEventListener(API_STATUS_ALERT_EVENT, handleApiStatusAlert);
    return () => window.removeEventListener(API_STATUS_ALERT_EVENT, handleApiStatusAlert);
  }, []);

  useEffect(() => {
    if (!alerta) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => setAlerta(null), 6500);
    return () => window.clearTimeout(timeoutId);
  }, [alerta]);

  if (!alerta) {
    return null;
  }

  const Icon = alerta.tipo === 'timeout' ? Clock : WifiOff;

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed right-4 top-4 z-[1000] flex w-[min(420px,calc(100vw-2rem))] items-start gap-3 rounded-xl border p-4 shadow-xl"
      style={{
        backgroundColor: 'var(--color-card)',
        borderColor: alerta.tipo === 'timeout' ? 'rgb(251 146 60)' : 'var(--color-border)',
        color: 'var(--color-text)',
      }}
    >
      <Icon
        size={18}
        className="mt-0.5 shrink-0"
        style={{ color: alerta.tipo === 'timeout' ? 'rgb(234 88 12)' : 'var(--color-text-muted)' }}
        aria-hidden="true"
      />
      <p className="min-w-0 flex-1 text-sm leading-relaxed">{alerta.mensagem}</p>
      <button
        type="button"
        onClick={() => setAlerta(null)}
        className="rounded-md p-1 transition-colors hover:bg-[var(--color-bg)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
        style={{ color: 'var(--color-text-muted)' }}
        aria-label="Fechar alerta"
      >
        <X size={15} aria-hidden="true" />
      </button>
    </div>
  );
}
