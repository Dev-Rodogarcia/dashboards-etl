import { createPortal } from 'react-dom';
import { useMemo, useState } from 'react';
import { Archive, Check, ClipboardList, Mail, UserRound, X } from 'lucide-react';
import type { HomeRequest } from '../../types/home';

const focusRingClass = 'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';
const TYPE_LABELS = { MELHORIA: 'Melhoria', AUTOMACAO: 'Automação', DASHBOARD: 'Dashboard', CORRECAO: 'Correção', OUTRO: 'Outro' } as const;

export default function HomeRequestsAdminPanel({
  open,
  onClose,
  requests,
  isLoading,
  saving,
  error,
  onComplete,
  onArchive,
}: {
  open: boolean;
  onClose: () => void;
  requests: HomeRequest[];
  isLoading: boolean;
  saving: boolean;
  error: string | null;
  onComplete: (id: string) => void;
  onArchive: (id: string) => void;
}) {
  const [showCompleted, setShowCompleted] = useState(false);
  const visibleRequests = useMemo(() => requests.filter((request) => showCompleted || request.status === 'ABERTA'), [requests, showCompleted]);
  if (!open || typeof document === 'undefined') return null;

  return createPortal(
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-[2px]" role="presentation" onMouseDown={onClose}>
      <section role="dialog" aria-modal="true" aria-label="Solicitações de melhoria" onMouseDown={(event) => event.stopPropagation()} className="flex max-h-[min(48rem,calc(100vh-2rem))] w-full max-w-4xl flex-col overflow-hidden rounded-[28px] border shadow-2xl" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
        <header className="flex items-center justify-between gap-4 border-b px-5 py-4" style={{ borderColor: 'var(--color-border)' }}>
          <div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl" style={{ backgroundColor: 'rgba(33, 71, 138, 0.14)', color: 'var(--color-primary)' }}><ClipboardList size={18} /></span><div><h2 className="text-lg font-extrabold" style={{ color: 'var(--color-text)' }}>Solicitações de melhoria</h2><p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>{requests.filter((request) => request.status === 'ABERTA').length} pendente(s) para avaliação</p></div></div>
          <div className="flex items-center gap-3"><button type="button" onClick={() => setShowCompleted((current) => !current)} className={`text-xs font-bold ${focusRingClass}`} style={{ color: 'var(--color-primary)' }}>{showCompleted ? 'Ocultar concluídas' : 'Ver concluídas'}</button><button type="button" onClick={onClose} className={`flex h-9 w-9 items-center justify-center rounded-xl border ${focusRingClass}`} style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }} aria-label="Fechar solicitações"><X size={17} /></button></div>
        </header>
        <div className="grid flex-1 gap-3 overflow-y-auto p-5 md:grid-cols-2">
          {error && <p className="md:col-span-2 rounded-xl px-3 py-2 text-xs" style={{ backgroundColor: 'rgba(239, 68, 68, 0.10)', color: '#dc2626' }}>{error}</p>}
          {visibleRequests.map((request) => (
            <article key={request.id} className="rounded-2xl border p-4" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
              <div className="flex items-start justify-between gap-2"><span className="rounded-full px-2 py-0.5 text-[10px] font-bold" style={{ backgroundColor: request.status === 'ABERTA' ? 'rgba(249, 115, 22, 0.14)' : 'rgba(16, 185, 129, 0.12)', color: request.status === 'ABERTA' ? '#ea580c' : '#059669' }}>{request.status === 'ABERTA' ? TYPE_LABELS[request.tipo] : 'Concluída'}</span><span className="text-[10px]" style={{ color: 'var(--color-text-muted)' }}>{new Date(request.criadoEm).toLocaleDateString('pt-BR')}</span></div>
              <h3 className="mt-3 text-sm font-bold" style={{ color: 'var(--color-text)' }}>{request.titulo}</h3><p className="mt-1 text-xs leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>{request.descricao}</p>
              {request.expectedResult && <p className="mt-3 border-l-2 pl-2 text-[11px]" style={{ borderColor: 'var(--color-primary)', color: 'var(--color-text-muted)' }}>Resultado esperado: {request.expectedResult}</p>}
              <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-[11px]" style={{ color: 'var(--color-text-muted)' }}><span className="inline-flex items-center gap-1"><UserRound size={12} />{request.solicitanteNome}</span><span className="inline-flex items-center gap-1"><Mail size={12} />{request.solicitanteEmail}</span></div>
              <div className="mt-4 flex gap-2">{request.status === 'ABERTA' && <button type="button" disabled={saving} onClick={() => onComplete(request.id)} className={`inline-flex h-8 items-center gap-1 rounded-lg border px-2 text-[11px] font-bold disabled:opacity-60 ${focusRingClass}`} style={{ borderColor: 'rgba(16, 185, 129, 0.35)', color: '#059669' }}><Check size={13} />Marcar feita</button>}<button type="button" disabled={saving} onClick={() => onArchive(request.id)} className={`inline-flex h-8 items-center gap-1 rounded-lg border px-2 text-[11px] font-bold disabled:opacity-60 ${focusRingClass}`} style={{ borderColor: 'rgba(239, 68, 68, 0.35)', color: '#dc2626' }}><Archive size={13} />Arquivar</button></div>
            </article>
          ))}
          {!isLoading && visibleRequests.length === 0 && <p className="md:col-span-2 py-12 text-center text-sm" style={{ color: 'var(--color-text-subtle)' }}>Nenhuma solicitação nesta visão.</p>}
          {isLoading && <p className="md:col-span-2 py-12 text-center text-sm" style={{ color: 'var(--color-text-subtle)' }}>Carregando solicitações...</p>}
        </div>
      </section>
    </div>,
    document.body,
  );
}
