import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { ReactNode } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { X } from 'lucide-react';

const SURFACE_STYLE = {
  backgroundColor: 'var(--color-card)',
  borderColor: 'var(--color-border)',
};

const SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

interface EslModalFrameProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  onClose: () => void;
  isBusy?: boolean;
  maxWidthClassName?: string;
}

/** Moldura compartilhada para operações síncronas do ESL, sem persistir qualquer estado fora do componente. */
export default function EslModalFrame({
  title,
  subtitle,
  children,
  onClose,
  isBusy = false,
  maxWidthClassName = 'max-w-6xl',
}: EslModalFrameProps) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isBusy) {
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
  }, [isBusy, onClose]);

  if (typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <AnimatePresence>
      <motion.div
        key={`esl-backdrop-${title}`}
        className="fixed inset-0 z-[80] bg-slate-950/45 backdrop-blur-[2px]"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={() => {
          if (!isBusy) {
            onClose();
          }
        }}
      />
      <motion.section
        key={`esl-dialog-${title}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 18 }}
        className={`fixed inset-x-4 bottom-4 top-4 z-[81] mx-auto flex w-full flex-col overflow-hidden rounded-[28px] border shadow-2xl ${maxWidthClassName}`}
        style={SURFACE_STYLE}
      >
        <header className="flex items-start justify-between gap-4 border-b px-5 py-4 sm:px-6" style={{ borderColor: 'var(--color-border)' }}>
          <div className="min-w-0">
            <p className="text-sm font-semibold" style={{ color: 'var(--color-primary)' }}>
              Operação ESL
            </p>
            <h2 className="mt-1 text-xl font-bold sm:text-2xl" style={{ color: 'var(--color-text)' }}>
              {title}
            </h2>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>
              {subtitle}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isBusy}
            className="rounded-xl border p-2 transition-opacity hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
            style={SECONDARY_BUTTON_STYLE}
            aria-label={`Fechar ${title}`}
          >
            <X size={18} />
          </button>
        </header>
        <div className="flex-1 overflow-y-auto px-5 py-5 sm:px-6">{children}</div>
      </motion.section>
    </AnimatePresence>,
    document.body,
  );
}
