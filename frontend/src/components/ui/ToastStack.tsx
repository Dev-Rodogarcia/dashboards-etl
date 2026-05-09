import { AnimatePresence, motion } from 'framer-motion';

export type ToastTone = 'success' | 'error' | 'info';

export interface ToastItem {
  id: string;
  message: string;
  tone: ToastTone;
}

const TONE_STYLE: Record<ToastTone, { background: string; border: string; color: string }> = {
  success: {
    background: 'var(--color-card)',
    border: '#16a34a',
    color: '#15803d',
  },
  error: {
    background: 'var(--color-card)',
    border: '#dc2626',
    color: '#dc2626',
  },
  info: {
    background: 'var(--color-card)',
    border: 'var(--color-primary)',
    color: 'var(--color-primary)',
  },
};

export default function ToastStack({ items }: { items: ToastItem[] }) {
  return (
    <div className="pointer-events-none fixed right-4 top-18 z-[90] flex w-full max-w-sm flex-col gap-3">
      <AnimatePresence initial={false}>
        {items.map((item) => {
          const style = TONE_STYLE[item.tone];
          return (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, y: -10, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8, scale: 0.98 }}
              transition={{ duration: 0.18 }}
              className="pointer-events-auto rounded-2xl border px-4 py-3 text-sm shadow-lg"
              style={{
                backgroundColor: style.background,
                borderColor: style.border,
                color: style.color,
              }}
            >
              {item.message}
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
