import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { Clock3, Loader2, Save, X } from 'lucide-react';
import type { ViagemJustificativaPayload } from '../../types/indicadoresGestaoAVista';
import { getApiErrorMessage } from '../../utils/apiError';

const SURFACE_STYLE = {
  backgroundColor: 'var(--color-card)',
  borderColor: 'var(--color-border)',
};

const FIELD_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

const SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

interface JustificativaHorarioCorteModalProps {
  codSolicitacao: number | null;
  isSubmitting?: boolean;
  onClose: () => void;
  onSubmit: (payload: ViagemJustificativaPayload) => Promise<void>;
}

interface JustificativaHorarioCorteModalAbertoProps {
  codSolicitacao: number;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (payload: ViagemJustificativaPayload) => Promise<void>;
}

export default function JustificativaHorarioCorteModal({
  codSolicitacao,
  isSubmitting = false,
  onClose,
  onSubmit,
}: JustificativaHorarioCorteModalProps) {
  if (codSolicitacao == null) {
    return null;
  }

  return (
    <JustificativaHorarioCorteModalAberto
      key={codSolicitacao}
      codSolicitacao={codSolicitacao}
      isSubmitting={isSubmitting}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}

function JustificativaHorarioCorteModalAberto({
  codSolicitacao,
  isSubmitting,
  onClose,
  onSubmit,
}: JustificativaHorarioCorteModalAbertoProps) {
  const [justificativa, setJustificativa] = useState('');
  const [erro, setErro] = useState('');

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSubmitting) {
        onClose();
      }
    }

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isSubmitting, onClose]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const texto = justificativa.trim();

    if (!texto) {
      setErro('Informe a justificativa da SM.');
      return;
    }

    try {
      setErro('');
      await onSubmit({ codSolicitacao, justificativa: texto });
    } catch (error) {
      setErro(getApiErrorMessage(error, 'Não foi possível registrar a justificativa.'));
    }
  }

  if (typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <AnimatePresence>
      <motion.div
        className="fixed inset-0 z-[80] bg-slate-950/45 backdrop-blur-[2px]"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={() => {
          if (!isSubmitting) onClose();
        }}
      />

      <motion.div
        role="dialog"
        aria-modal="true"
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 18 }}
        className="fixed inset-x-4 top-1/2 z-[81] mx-auto w-full max-w-xl -translate-y-1/2 overflow-hidden rounded-[24px] border shadow-2xl"
        style={SURFACE_STYLE}
      >
        <div className="flex items-start justify-between gap-4 border-b px-5 py-4" style={{ borderColor: 'var(--color-border)' }}>
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-sm font-semibold" style={{ color: 'var(--color-primary)' }}>
              <Clock3 size={18} />
              Justificativa de Horário de Corte
            </div>
            <h2 className="mt-1 text-xl font-bold" style={{ color: 'var(--color-text)' }}>
              SM {codSolicitacao}
            </h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-xl border p-2 transition-opacity hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
            style={SECONDARY_BUTTON_STYLE}
            aria-label="Fechar justificativa"
          >
            <X size={18} />
          </button>
        </div>

        <form onSubmit={(event) => void handleSubmit(event)} className="space-y-4 px-5 py-5">
          <label className="block space-y-2">
            <span className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
              Justificativa
            </span>
            <textarea
              value={justificativa}
              onChange={(event) => setJustificativa(event.target.value)}
              className="min-h-36 w-full resize-none rounded-xl border px-3 py-2.5 text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
              style={FIELD_STYLE}
              maxLength={1000}
              disabled={isSubmitting}
              required
            />
          </label>

          {erro ? (
            <p className="rounded-xl border px-3 py-2 text-sm" style={{ borderColor: '#dc2626', color: '#dc2626', backgroundColor: 'rgba(220, 38, 38, 0.08)' }}>
              {erro}
            </p>
          ) : null}

          <div className="flex flex-wrap justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="inline-flex h-10 items-center gap-2 rounded-xl border px-4 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50"
              style={SECONDARY_BUTTON_STYLE}
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex h-10 items-center gap-2 rounded-xl px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
              style={{ backgroundColor: 'var(--color-primary)' }}
            >
              {isSubmitting ? <Loader2 className="animate-spin" size={16} /> : <Save size={16} />}
              Salvar
            </button>
          </div>
        </form>
      </motion.div>
    </AnimatePresence>,
    document.body,
  );
}
