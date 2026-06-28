import { useEffect, useId } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import type { IntegracaoPendencia } from '../../../api/endpoints/integracoesServico';

interface CanhotoImagemModalProps {
  pendencia: IntegracaoPendencia | null;
  onClose: () => void;
}

function formatarIdentificacao(pendencia: IntegracaoPendencia): string {
  const numeroNf = pendencia.numeroNf != null ? `NF ${pendencia.numeroNf}` : 'NF nao informada';
  const serie = pendencia.serieNf ? `Serie ${pendencia.serieNf}` : null;
  return [numeroNf, serie].filter(Boolean).join(' - ');
}

export default function CanhotoImagemModal({
  pendencia,
  onClose,
}: CanhotoImagemModalProps) {
  const titleId = useId();

  useEffect(() => {
    if (!pendencia) {
      return undefined;
    }

    const overflowOriginal = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = overflowOriginal;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, pendencia]);

  if (!pendencia || typeof document === 'undefined') {
    return null;
  }

  const identificacao = formatarIdentificacao(pendencia);
  const canhotoReferencia = pendencia.canhotoReferencia?.trim() ?? '';
  const canhotoMimeType = pendencia.canhotoMimeType?.trim().toLowerCase() ?? '';
  const isPdf = canhotoMimeType === 'application/pdf'
    || canhotoReferencia.toLowerCase().startsWith('data:application/pdf');

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-3 sm:p-5"
      onMouseDown={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="flex max-h-[92vh] w-[min(1120px,96vw)] flex-col overflow-hidden rounded-lg border shadow-2xl"
        style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header
          className="flex min-h-14 items-center justify-between gap-3 border-b px-4 py-3"
          style={{ borderColor: 'var(--color-border)' }}
        >
          <div className="min-w-0">
            <h2 id={titleId} className="truncate text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
              Ver canhoto
            </h2>
            <p className="truncate text-xs" style={{ color: 'var(--color-text-muted)' }}>
              {pendencia.sistemaDestino} - {identificacao}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border transition-colors hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)',
            }}
            aria-label="Fechar modal"
            title="Fechar"
          >
            <X size={16} aria-hidden="true" />
          </button>
        </header>

        <div className="min-h-[180px] overflow-auto p-4 sm:p-5">
          {canhotoReferencia && (
            <div className="flex min-h-[180px] items-center justify-center">
              {isPdf ? (
                <object
                  data={canhotoReferencia}
                  type="application/pdf"
                  className="min-h-[70vh] w-full rounded-md border bg-white"
                  style={{ borderColor: 'var(--color-border)' }}
                  aria-label={`Canhoto ${identificacao}`}
                >
                  <a href={canhotoReferencia} target="_blank" rel="noreferrer">
                    Abrir PDF do canhoto
                  </a>
                </object>
              ) : (
                <img
                  src={canhotoReferencia}
                  alt={`Canhoto ${identificacao}`}
                  className="block rounded-md border bg-white"
                  style={{
                    maxWidth: '100%',
                    height: 'auto',
                    objectFit: 'contain',
                    borderColor: 'var(--color-border)',
                  }}
                />
              )}
            </div>
          )}

          {!canhotoReferencia && (
            <div
              className="flex min-h-[180px] items-center justify-center rounded-lg border px-4 py-8 text-center text-sm"
              style={{
                borderColor: 'var(--color-border)',
                color: 'var(--color-text-muted)',
                backgroundColor: 'var(--color-bg)',
              }}
            >
              Imagem nao disponivel para este registro.
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}
