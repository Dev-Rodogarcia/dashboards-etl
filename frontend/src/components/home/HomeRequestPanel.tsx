import { useState } from 'react';
import type { FormEvent } from 'react';
import { ClipboardList, Lightbulb, Paperclip, Send, X } from 'lucide-react';
import type { HomeRequestFormState, HomeRequestType } from '../../types/home';

const REQUEST_TYPES: Array<{ value: HomeRequestType; label: string }> = [
  { value: 'MELHORIA', label: 'Melhoria de processo' },
  { value: 'AUTOMACAO', label: 'Automação' },
  { value: 'DASHBOARD', label: 'Novo dashboard ou relatório' },
  { value: 'CORRECAO', label: 'Correção' },
  { value: 'OUTRO', label: 'Outro' },
];

const EMPTY_FORM: HomeRequestFormState = { type: 'MELHORIA', title: '', description: '', expectedResult: '', applicationLocation: '', attachments: [] };
const focusRingClass = 'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';
const MAX_ATTACHMENTS = 5;
const MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024;
const MAX_TOTAL_ATTACHMENTS_BYTES = 20 * 1024 * 1024;

export default function HomeRequestPanel({
  saving,
  error,
  requestCount,
  requestsLabel = 'Ver solicitações',
  onOpenRequests,
  onSubmit,
}: {
  saving: boolean;
  error: string | null;
  requestCount?: number;
  requestsLabel?: string;
  onOpenRequests?: () => void;
  onSubmit: (form: HomeRequestFormState) => Promise<void>;
}) {
  const [form, setForm] = useState<HomeRequestFormState>(EMPTY_FORM);
  const [submitted, setSubmitted] = useState(false);
  const [attachmentError, setAttachmentError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await onSubmit(form);
      setForm(EMPTY_FORM);
      setAttachmentError(null);
      setSubmitted(true);
    } catch {
      // O erro é apresentado no próprio formulário.
    }
  }

  function selectAttachments(files: FileList | null) {
    const selected = Array.from(files ?? []);
    if (selected.length === 0) return;

    const attachments = [...form.attachments, ...selected];
    if (attachments.length > MAX_ATTACHMENTS) {
      setAttachmentError('Envie no máximo 5 anexos por solicitação.');
      return;
    }
    if (attachments.some((attachment) => attachment.size > MAX_ATTACHMENT_BYTES)) {
      setAttachmentError('Cada anexo pode ter no máximo 10 MB.');
      return;
    }
    if (attachments.reduce((total, attachment) => total + attachment.size, 0) > MAX_TOTAL_ATTACHMENTS_BYTES) {
      setAttachmentError('Os anexos somados podem ter no máximo 20 MB.');
      return;
    }

    setForm({ ...form, attachments });
    setAttachmentError(null);
  }

  return (
    <section className="overflow-hidden rounded-[24px] border shadow-[0_14px_30px_rgba(5,150,105,0.10)]" style={{ backgroundColor: 'var(--color-card)', borderColor: 'rgba(16, 185, 129, 0.38)' }} aria-label="Enviar solicitação de melhoria">
      <div className="grid gap-5 p-5 2xl:grid-cols-[minmax(17rem,0.9fr)_minmax(0,1.55fr)] 2xl:items-stretch">
        <div className="rounded-[20px] p-5 2xl:flex 2xl:h-full 2xl:flex-col 2xl:p-6" style={{ background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.24), rgba(5, 150, 105, 0.08))', border: '1px solid rgba(16, 185, 129, 0.20)' }}>
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl shadow-sm 2xl:h-14 2xl:w-14" style={{ backgroundColor: '#059669', color: '#fff' }}><Lightbulb size={22} /></span>
          <p className="mt-5 text-[11px] font-bold uppercase tracking-[0.14em]" style={{ color: '#047857' }}>Melhoria contínua</p>
          <h2 className="mt-1 text-xl font-extrabold leading-tight 2xl:text-2xl" style={{ color: 'var(--color-text)' }}>O que podemos facilitar na sua rotina?</h2>
          <p className="mt-2 max-w-sm text-sm leading-relaxed 2xl:text-[15px]" style={{ color: 'var(--color-text-subtle)' }}>Registre a ideia com o contexto. A equipe recebe informações suficientes para avaliar a melhor solução.</p>
          {onOpenRequests && (
            <button type="button" onClick={onOpenRequests} className={`mt-6 inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-bold transition-colors hover:bg-white/40 2xl:mt-auto 2xl:h-11 2xl:w-full 2xl:justify-center 2xl:px-4 2xl:text-sm ${focusRingClass}`} style={{ borderColor: 'rgba(5, 150, 105, 0.30)', color: '#047857' }}>
              <ClipboardList size={15} />
              {requestsLabel}{requestCount != null ? ` (${requestCount})` : ''}
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
          <label className="space-y-1 md:row-span-2">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Contexto e impacto</span>
            <textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} maxLength={2000} required className={`h-28 min-h-28 max-h-44 w-full resize-y rounded-xl border px-3 py-2 text-sm md:h-[8.5rem] ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }} placeholder="Como é feito hoje e qual dificuldade isso resolve?" />
          </label>
          <label className="space-y-1 md:col-start-2">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Resultado esperado <span className="normal-case font-medium">(opcional)</span></span>
            <input value={form.expectedResult} onChange={(event) => setForm({ ...form, expectedResult: event.target.value })} maxLength={1000} className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }} placeholder="Como seria a solução ideal para você?" />
          </label>
          <label className="space-y-1 md:col-start-2">
            <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Onde será aplicado? <span className="normal-case font-medium">(opcional)</span></span>
            <input value={form.applicationLocation} onChange={(event) => setForm({ ...form, applicationLocation: event.target.value })} maxLength={500} className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`} style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }} placeholder="Ex.: dashboard de Faturamento, tela de Coletas ou rotina de emissão de manifestos" />
          </label>
          <div className="flex flex-wrap items-end justify-between gap-3 pt-1 md:col-span-2">
            <div className="min-w-0 flex-1 space-y-2">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Anexos <span className="normal-case font-medium">(opcional)</span></span>
                <span className="text-[10px]" style={{ color: 'var(--color-text-muted)' }}>PDF, PNG/JPG, XLS/XLSX ou CSV · até 5 arquivos</span>
              </div>
              <label className={`inline-flex h-9 cursor-pointer items-center gap-2 rounded-xl border px-3 text-xs font-bold transition-colors hover:bg-white/40 ${focusRingClass}`} style={{ borderColor: 'rgba(5, 150, 105, 0.30)', color: '#047857' }}>
                <Paperclip size={14} />
                Adicionar anexos
                <input type="file" multiple accept=".pdf,.png,.jpg,.jpeg,.xls,.xlsx,.csv,application/pdf,image/png,image/jpeg,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/csv" className="sr-only" onChange={(event) => { selectAttachments(event.target.files); event.currentTarget.value = ''; }} />
              </label>
              {form.attachments.length > 0 && <div className="flex flex-wrap gap-2">{form.attachments.map((attachment, index) => <span key={`${attachment.name}-${attachment.size}-${index}`} className="inline-flex max-w-full items-center gap-1 rounded-lg border px-2 py-1 text-[11px]" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-subtle)' }}><Paperclip size={12} /><span className="max-w-52 truncate">{attachment.name}</span><button type="button" onClick={() => setForm({ ...form, attachments: form.attachments.filter((_, attachmentIndex) => attachmentIndex !== index) })} className={`rounded p-0.5 hover:bg-black/5 ${focusRingClass}`} aria-label={`Remover ${attachment.name}`}><X size={12} /></button></span>)}</div>}
              {attachmentError && <p className="text-xs" style={{ color: '#dc2626' }}>{attachmentError}</p>}
              {error && <p className="rounded-xl px-3 py-2 text-xs" style={{ backgroundColor: 'rgba(239, 68, 68, 0.10)', color: '#dc2626' }}>{error}</p>}
              {submitted && !error && <p className="text-xs font-medium" style={{ color: '#059669' }}>Solicitação enviada. Obrigado por ajudar a melhorar o portal.</p>}
            </div>
            <button type="submit" disabled={saving} className={`inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl px-4 text-xs font-bold text-white disabled:opacity-60 ${focusRingClass}`} style={{ backgroundColor: '#059669' }}><Send size={14} />{saving ? 'Enviando...' : 'Enviar solicitação'}</button>
          </div>
        </form>
      </div>
    </section>
  );
}
