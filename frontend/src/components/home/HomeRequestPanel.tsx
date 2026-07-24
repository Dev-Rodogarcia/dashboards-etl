import { useState } from 'react';
import type { FormEvent } from 'react';
import { ClipboardList, Lightbulb, Send } from 'lucide-react';
import type { HomeRequestFormState, HomeRequestType } from '../../types/home';

const REQUEST_TYPES: Array<{ value: HomeRequestType; label: string }> = [
  { value: 'MELHORIA', label: 'Melhoria de processo' },
  { value: 'AUTOMACAO', label: 'Automação' },
  { value: 'DASHBOARD', label: 'Novo dashboard ou relatório' },
  { value: 'CORRECAO', label: 'Correção' },
  { value: 'OUTRO', label: 'Outro' },
];

const EMPTY_FORM: HomeRequestFormState = { type: 'MELHORIA', title: '', description: '', expectedResult: '' };
const focusRingClass = 'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

export default function HomeRequestPanel({
  saving,
  error,
  pendingCount,
  onOpenRequests,
  onSubmit,
}: {
  saving: boolean;
  error: string | null;
  pendingCount?: number;
  onOpenRequests?: () => void;
  onSubmit: (form: HomeRequestFormState) => Promise<void>;
}) {
  const [form, setForm] = useState<HomeRequestFormState>(EMPTY_FORM);
  const [submitted, setSubmitted] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await onSubmit(form);
      setForm(EMPTY_FORM);
      setSubmitted(true);
    } catch {
      // O erro é apresentado no próprio formulário.
    }
  }

  return (
    <section className="overflow-hidden rounded-[24px] border shadow-[0_14px_30px_rgba(5,150,105,0.10)]" style={{ backgroundColor: 'var(--color-card)', borderColor: 'rgba(16, 185, 129, 0.38)' }} aria-label="Enviar solicitação de melhoria">
      <div className="grid gap-5 p-5 2xl:grid-cols-[minmax(17rem,0.9fr)_minmax(0,1.55fr)] 2xl:items-stretch">
        <div className="self-stretch rounded-[20px] p-5" style={{ background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.24), rgba(5, 150, 105, 0.08))', border: '1px solid rgba(16, 185, 129, 0.20)' }}>
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl shadow-sm" style={{ backgroundColor: '#059669', color: '#fff' }}><Lightbulb size={21} /></span>
          <p className="mt-5 text-[11px] font-bold uppercase tracking-[0.14em]" style={{ color: '#047857' }}>Melhoria contínua</p>
          <h2 className="mt-1 text-xl font-extrabold leading-tight" style={{ color: 'var(--color-text)' }}>O que podemos facilitar na sua rotina?</h2>
          <p className="mt-2 max-w-sm text-sm leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>Registre a ideia com o contexto. A equipe recebe informações suficientes para avaliar a melhor solução.</p>
          {onOpenRequests && (
            <button type="button" onClick={onOpenRequests} className={`mt-6 inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-bold transition-colors hover:bg-white/40 ${focusRingClass}`} style={{ borderColor: 'rgba(5, 150, 105, 0.30)', color: '#047857' }}>
              <ClipboardList size={15} />
              Ver solicitações{pendingCount != null ? ` (${pendingCount} pendente${pendingCount === 1 ? '' : 's'})` : ''}
            </button>
          )}
        </div>

        <form onSubmit={(event) => void submit(event)} className="grid gap-3 rounded-[20px] border p-4 md:grid-cols-2" style={{ backgroundColor: 'rgba(33, 71, 138, 0.035)', borderColor: 'var(--color-border)' }}>
          <label className="space-y-1">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Tipo de melhoria</span>
            <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as HomeRequestType })} className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}>
              {REQUEST_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
            </select>
          </label>
          <label className="space-y-1">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>O que você precisa?</span>
            <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={140} required className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }} placeholder="Ex.: automatizar conferência diária" />
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Contexto e impacto</span>
            <textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} maxLength={2000} required className={`min-h-20 w-full resize-y rounded-xl border px-3 py-2 text-sm ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }} placeholder="Como é feito hoje e qual dificuldade isso resolve?" />
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Resultado esperado <span className="normal-case font-medium">(opcional)</span></span>
            <input value={form.expectedResult} onChange={(event) => setForm({ ...form, expectedResult: event.target.value })} maxLength={1000} className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }} placeholder="Como seria a solução ideal para você?" />
          </label>
          <div className="flex flex-wrap items-center justify-between gap-3 pt-1 md:col-span-2">
            <div>{error && <p className="rounded-xl px-3 py-2 text-xs" style={{ backgroundColor: 'rgba(239, 68, 68, 0.10)', color: '#dc2626' }}>{error}</p>}{submitted && !error && <p className="text-xs font-medium" style={{ color: '#059669' }}>Solicitação enviada. Obrigado por ajudar a melhorar o portal.</p>}</div>
            <button type="submit" disabled={saving} className={`inline-flex h-10 items-center justify-center gap-2 rounded-xl px-4 text-xs font-bold text-white disabled:opacity-60 ${focusRingClass}`} style={{ backgroundColor: '#059669' }}><Send size={14} />{saving ? 'Enviando...' : 'Enviar solicitação'}</button>
          </div>
        </form>
      </div>
    </section>
  );
}
