import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Archive, Bell, CircleAlert, Megaphone, Pencil, Plus, Save, Sparkles, X } from 'lucide-react';
import type {
  HomeCommunicationPriority,
  HomeCommunicationTab,
  HomeNotice,
  HomeNoticeFormState,
  HomeNoticeTag,
} from '../../types/home';

const focusRingClass =
  'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

const TAB_LABELS: Record<HomeCommunicationTab, string> = {
  avisos: 'Avisos',
  atualizacoes: 'Atualizações',
  pendencias: 'Pendências',
};

const TAB_ICONS = {
  avisos: Bell,
  atualizacoes: Sparkles,
  pendencias: CircleAlert,
} as const;

function tagLabel(tag: HomeNoticeTag): string {
  const labels: Record<HomeNoticeTag, string> = {
    NOVO: 'Novo',
    ATENCAO: 'Atenção',
    FIXADO: 'Fixado',
  };

  return labels[tag];
}

function priorityForNotice(notice: HomeNotice): HomeCommunicationPriority {
  if (notice.tag === 'ATENCAO') return 'Alta';
  if (notice.tag === 'FIXADO') return 'Média';
  return 'Baixa';
}

function priorityStyle(priority: HomeCommunicationPriority) {
  if (priority === 'Alta') {
    return { backgroundColor: 'rgba(220, 38, 38, 0.14)', color: '#dc2626' };
  }

  if (priority === 'Média') {
    return { backgroundColor: 'rgba(249, 115, 22, 0.16)', color: '#ea580c' };
  }

  return { backgroundColor: 'rgba(22, 163, 74, 0.14)', color: '#15803d' };
}

function filterByTab(notices: HomeNotice[], tab: HomeCommunicationTab) {
  if (tab === 'atualizacoes') {
    return notices.filter((notice) => notice.tag === 'NOVO');
  }

  if (tab === 'pendencias') {
    return notices.filter((notice) => notice.tag === 'ATENCAO');
  }

  return notices;
}

export default function CommunicationsPanel({
  notices,
  isLoading,
  error,
  isAdmin,
  form,
  showForm,
  editingNoticeId,
  saving,
  onFormChange,
  onStartCreate,
  onStartEdit,
  onCancelForm,
  onSubmit,
  onArchive,
  className = '',
}: {
  notices: HomeNotice[];
  isLoading: boolean;
  error: string | null;
  isAdmin: boolean;
  form: HomeNoticeFormState;
  showForm: boolean;
  editingNoticeId: string | null;
  saving: boolean;
  onFormChange: (form: HomeNoticeFormState) => void;
  onStartCreate: () => void;
  onStartEdit: (notice: HomeNotice) => void;
  onCancelForm: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onArchive: (id: string) => void;
  className?: string;
}) {
  const [activeTab, setActiveTab] = useState<HomeCommunicationTab>('avisos');
  const [selectedNoticeId, setSelectedNoticeId] = useState<string | null>(null);

  const filteredNotices = useMemo(() => filterByTab(notices, activeTab), [activeTab, notices]);
  const visibleNotices = filteredNotices;

  function handleTabChange(tab: HomeCommunicationTab) {
    setActiveTab(tab);
    setSelectedNoticeId(null);
  }

  return (
    <aside
      className={`flex ${className}`}
      aria-label="Comunicações"
    >
      <section
        className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-[24px] border shadow-[0_16px_34px_rgba(33,71,138,0.13)]"
        style={{
          backgroundColor: 'var(--color-card)',
          borderColor: 'rgba(33, 71, 138, 0.38)',
        }}
      >
        <div
          className="sticky top-0 z-10 border-b px-4 py-4"
          style={{
            backgroundColor: 'var(--color-card)',
            borderColor: 'var(--color-border)',
          }}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.14em]" style={{ color: '#1d4ed8' }}>
                Command Center
              </p>
              <h2 className="mt-1 text-lg font-extrabold" style={{ color: 'var(--color-text)' }}>
                Comunicações
              </h2>
              <p className="mt-1 text-xs leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
                Avisos, atualizações e pendências internas.
              </p>
            </div>
            <span
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border"
              style={{
                backgroundColor: 'rgba(33, 71, 138, 0.14)',
                borderColor: 'var(--color-primary)',
                color: 'var(--color-primary)',
              }}
            >
              <Bell size={18} />
            </span>
          </div>

          <div className="mt-4 grid grid-cols-3 gap-2">
            {(Object.keys(TAB_LABELS) as HomeCommunicationTab[]).map((tab) => {
              const active = activeTab === tab;
              const TabIcon = TAB_ICONS[tab];
              return (
                <button
                  key={tab}
                  type="button"
                  onClick={() => handleTabChange(tab)}
                  className={`flex h-10 items-center justify-center gap-1.5 rounded-xl border text-[10px] font-bold transition-all duration-200 ${focusRingClass}`}
                  style={{
                    backgroundColor: active ? 'var(--color-primary)' : 'rgba(33, 71, 138, 0.05)',
                    borderColor: active ? 'var(--color-primary)' : 'rgba(33, 71, 138, 0.16)',
                    color: active ? '#FFFFFF' : 'var(--color-text)',
                  }}
                  aria-pressed={active}
                >
                  <TabIcon size={13} />
                  {TAB_LABELS[tab]}
                </button>
              );
            })}
          </div>

          {isAdmin && (
            <button
              type="button"
              onClick={onStartCreate}
            className={`mt-3 inline-flex h-9 w-full items-center justify-center gap-2 rounded-xl px-3 text-xs font-bold text-white transition-all duration-200 hover:-translate-y-0.5 hover:opacity-95 ${focusRingClass}`}
              style={{ backgroundColor: 'var(--color-primary)' }}
            >
              <Plus size={14} />
              Novo comunicado
            </button>
          )}
        </div>

        <div
          className="communications-scroll max-h-[39rem] flex-1 overflow-y-auto px-4 py-4"
        >
          {showForm && isAdmin && (
            <div
              className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"
              role="presentation"
              onMouseDown={(event) => {
                if (event.target === event.currentTarget) onCancelForm();
              }}
            >
            <form
              onSubmit={onSubmit}
              className="max-h-[calc(100vh-2rem)] w-full max-w-xl space-y-3 overflow-y-auto rounded-[22px] border p-5 shadow-2xl"
              style={{
                backgroundColor: 'var(--color-card)',
                borderColor: 'var(--color-border)',
              }}
            >
              <label className="space-y-1">
                <span className="text-xs font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Título</span>
                <input
                  value={form.title}
                  onChange={(event) => onFormChange({ ...form, title: event.target.value })}
                  className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`}
                  style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                  maxLength={140}
                  required
                />
              </label>

              <div className="grid gap-3 2xl:grid-cols-[1fr_1.25fr]">
                <label className="space-y-1">
                  <span className="text-xs font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Tipo</span>
                  <select
                    value={form.tag}
                    onChange={(event) => onFormChange({ ...form, tag: event.target.value as HomeNoticeTag })}
                    className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`}
                    style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                  >
                    <option value="ATENCAO">Atenção</option>
                    <option value="NOVO">Novo</option>
                    <option value="FIXADO">Fixado</option>
                  </select>
                </label>

                <label className="space-y-1">
                  <span className="text-xs font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Público</span>
                  <input
                    value={form.audience}
                    onChange={(event) => onFormChange({ ...form, audience: event.target.value })}
                    className={`h-10 w-full rounded-xl border px-3 text-sm ${focusRingClass}`}
                    style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                    maxLength={140}
                    required
                  />
                </label>
              </div>

              <label className="space-y-1">
                <span className="text-xs font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>Mensagem</span>
                <textarea
                  value={form.body}
                  onChange={(event) => onFormChange({ ...form, body: event.target.value })}
                  className={`min-h-24 w-full resize-none rounded-xl border px-3 py-2 text-sm ${focusRingClass}`}
                  style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
                  maxLength={700}
                  required
                />
              </label>

              <div className="flex flex-wrap gap-2">
                <button
                  type="submit"
                  disabled={saving}
                  className={`inline-flex h-9 items-center gap-2 rounded-xl px-3 text-xs font-bold text-white disabled:cursor-not-allowed disabled:opacity-60 ${focusRingClass}`}
                  style={{ backgroundColor: 'var(--color-primary)' }}
                >
                  <Save size={14} />
                  {editingNoticeId ? 'Salvar' : 'Publicar'}
                </button>
                <button
                  type="button"
                  onClick={onCancelForm}
                  className={`inline-flex h-9 items-center gap-2 rounded-xl border px-3 text-xs font-bold transition-colors hover:bg-[var(--color-bg)] ${focusRingClass}`}
                  style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
                >
                  <X size={14} />
                  Cancelar
                </button>
              </div>
            </form>
            </div>
          )}

          {error && (
            <p
              className="mb-4 rounded-xl border px-3 py-2 text-xs"
              style={{
                borderColor: 'rgba(239, 68, 68, 0.35)',
                color: '#dc2626',
                backgroundColor: 'rgba(239, 68, 68, 0.08)',
              }}
            >
              {error}
            </p>
          )}

          <div className="space-y-3" role="list">
            {visibleNotices.map((notice) => {
              const priority = priorityForNotice(notice);
              const selected = selectedNoticeId === notice.id;
              const isNew = notice.tag === 'NOVO';

              return (
                <article key={notice.id} role="listitem">
                  <button
                    type="button"
                    onClick={() => setSelectedNoticeId(selected ? null : notice.id)}
                    className={`group w-full rounded-[20px] border p-4 text-left shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md ${focusRingClass}`}
                    style={{
                      backgroundColor: selected
                        ? 'var(--color-bg)'
                        : 'var(--color-card)',
                      borderColor: selected ? 'var(--color-primary)' : 'var(--color-border)',
                    }}
                    aria-expanded={selected}
                  >
                    <div className="mb-2 flex items-center justify-between gap-3">
                      <div className="flex min-w-0 items-center gap-2">
                        {isNew && (
                          <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: 'var(--color-primary)' }} aria-label="Novo" />
                        )}
                        <span className="truncate text-[11px] font-bold uppercase" style={{ color: 'var(--color-text-muted)' }}>
                          {tagLabel(notice.tag)}
                        </span>
                      </div>
                      <span className="shrink-0 rounded-full px-2 py-1 text-[10px] font-bold uppercase" style={priorityStyle(priority)}>
                        {priority}
                      </span>
                    </div>

                    <h3 className="text-sm font-bold leading-snug" style={{ color: 'var(--color-text)' }}>
                      {notice.title}
                    </h3>
                    <p className="mt-2 line-clamp-3 text-sm leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
                      {notice.body}
                    </p>
                    <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px]" style={{ color: 'var(--color-text-muted)' }}>
                      <span>{notice.date}</span>
                      <span aria-hidden="true">|</span>
                      <span>{notice.audience}</span>
                    </div>
                  </button>

                  {selected && isAdmin && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => onStartEdit(notice)}
                        className={`inline-flex h-8 items-center gap-1.5 rounded-lg border px-2.5 text-xs font-bold transition-colors hover:bg-[var(--color-bg)] ${focusRingClass}`}
                        style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
                      >
                        <Pencil size={13} />
                        Editar
                      </button>
                      <button
                        type="button"
                        onClick={() => onArchive(notice.id)}
                        disabled={saving}
                        className={`inline-flex h-8 items-center gap-1.5 rounded-lg border px-2.5 text-xs font-bold text-red-600 transition-colors hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60 dark:hover:bg-red-950/30 ${focusRingClass}`}
                        style={{ borderColor: 'rgba(239, 68, 68, 0.35)' }}
                      >
                        <Archive size={13} />
                        Arquivar
                      </button>
                    </div>
                  )}
                </article>
              );
            })}

            {visibleNotices.length === 0 && !isLoading && (
              <div className="rounded-[20px] border px-5 py-8 text-center" style={{ borderColor: 'var(--color-border)' }}>
                <Megaphone className="mx-auto mb-3" size={22} style={{ color: 'var(--color-text-muted)' }} />
                <p className="text-sm font-bold" style={{ color: 'var(--color-text)' }}>Nenhum item nesta visão</p>
                <p className="mt-1 text-xs" style={{ color: 'var(--color-text-subtle)' }}>Troque de aba ou publique um comunicado.</p>
              </div>
            )}

            {isLoading && (
              <div className="rounded-[20px] border px-5 py-8 text-center text-sm" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-subtle)' }}>
                Carregando comunicações...
              </div>
            )}
          </div>
        </div>

      </section>
    </aside>
  );
}
