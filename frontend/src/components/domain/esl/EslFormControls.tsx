import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
import { AlertCircle } from 'lucide-react';

const FIELD_BASE_CLASS = 'w-full rounded-xl border px-3 py-2.5 text-sm outline-none transition focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 disabled:cursor-not-allowed disabled:opacity-60';

const FIELD_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

export const ESL_PRIMARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-primary)',
  color: '#fff',
};

export const ESL_SECONDARY_BUTTON_STYLE = {
  backgroundColor: 'var(--color-bg)',
  borderColor: 'var(--color-border)',
  color: 'var(--color-text)',
};

interface FieldBaseProps {
  label: string;
  error?: string;
  hint?: string;
  className?: string;
}

export function EslInput({ label, error, hint, className, ...props }: FieldBaseProps & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className={`block space-y-1.5 ${className ?? ''}`}>
      <span className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{label}</span>
      <input
        {...props}
        aria-invalid={Boolean(error)}
        className={FIELD_BASE_CLASS}
        style={{ ...FIELD_STYLE, borderColor: error ? '#dc2626' : FIELD_STYLE.borderColor }}
      />
      {hint && !error ? <span className="block text-xs" style={{ color: 'var(--color-text-subtle)' }}>{hint}</span> : null}
      {error ? <FieldError message={error} /> : null}
    </label>
  );
}

export function EslSelect({
  label,
  error,
  hint,
  className,
  children,
  ...props
}: FieldBaseProps & SelectHTMLAttributes<HTMLSelectElement> & { children: ReactNode }) {
  return (
    <label className={`block space-y-1.5 ${className ?? ''}`}>
      <span className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{label}</span>
      <select
        {...props}
        aria-invalid={Boolean(error)}
        className={FIELD_BASE_CLASS}
        style={{ ...FIELD_STYLE, borderColor: error ? '#dc2626' : FIELD_STYLE.borderColor }}
      >
        {children}
      </select>
      {hint && !error ? <span className="block text-xs" style={{ color: 'var(--color-text-subtle)' }}>{hint}</span> : null}
      {error ? <FieldError message={error} /> : null}
    </label>
  );
}

export function EslTextarea({ label, error, hint, className, ...props }: FieldBaseProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <label className={`block space-y-1.5 ${className ?? ''}`}>
      <span className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{label}</span>
      <textarea
        {...props}
        aria-invalid={Boolean(error)}
        className={`${FIELD_BASE_CLASS} min-h-24 resize-y`}
        style={{ ...FIELD_STYLE, borderColor: error ? '#dc2626' : FIELD_STYLE.borderColor }}
      />
      {hint && !error ? <span className="block text-xs" style={{ color: 'var(--color-text-subtle)' }}>{hint}</span> : null}
      {error ? <FieldError message={error} /> : null}
    </label>
  );
}

export function EslErrorPanel({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }

  return (
    <div className="flex items-start gap-2 rounded-xl border px-3 py-2.5 text-sm" style={{ borderColor: '#dc2626', color: '#b91c1c', backgroundColor: 'rgba(220, 38, 38, 0.08)' }}>
      <AlertCircle size={16} className="mt-0.5 shrink-0" />
      <span>{message}</span>
    </div>
  );
}

function FieldError({ message }: { message: string }) {
  return <span className="block text-xs font-medium" style={{ color: '#b91c1c' }}>{message}</span>;
}
