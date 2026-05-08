import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Archive, Bell, CheckCircle2, Megaphone, Pencil, Plus, Save, X } from 'lucide-react';
import type {
  HomeCommunicationPriority,
  HomeCommunicationTab,
  HomeNotice,
  HomeNoticeFormState,
  HomeNoticeTag,
} from '../../types/home';

const focusRingClass =
  'outline-none focus-visible:ring-2 focus-visible:ring-[color-mix(in_srgb,var(--color-primary)_34%,transparent)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-bg)]';

const TAB_LABELS: Record<HomeCommunicationTab, string> = {
  avisos: 'Avisos',
  atualizacoes: 'Atualizações',
  pendencias: 'Pendências',
};

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
    return { backgroundColor: 'rgba(239, 68, 68, 0.20)', color: '#b91c1c' };
  }

  if (priority === 'Média') {
    return { backgroundColor: 'rgba(245, 158, 11, 0.22)', color: '#a16207' };
  }

  return { backgroundColor: 'rgba(16, 185, 129, 0.20)', color: '#047857' };
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
  const [showAll, setShowAll] = useState(false);

  const filteredNotices = useMemo(() => filterByTab(notices, activeTab), [activeTab, notices]);
  const visibleNotices = showAll ? filteredNotices : filteredNotices.slice(0, 5);

  function handleTabChange(tab: HomeCommunicationTab) {
    setActiveTab(tab);
    setShowAll(false);
    setSelectedNoticeId(null);
  }

  return (
    <aside
      className={`flex ${className}`}
      aria-label="Comunicações"
    >
      <section
        className="flex min-h-[38rem] flex-1 flex-col overflow-hidden rounded-[30px] border shadow-[0_22px_48px_rgba(15,23,42,0.10)]"
        style={{
          backgroundColor: 'color-mix(in srgb, var(--color-card) 94%, var(--color-bg))',
          borderColor: 'color-mix(in srgb, var(--color-border) 76%, transparent)',
        }}
      >
        <div
          className="sticky top-0 z-10 border-b px-5 py-5 backdrop-blur"
          style={{
            backgroundColor: 'color-mix(in srgb, var(--color-card) 88%, transparent)',
            borderColor: 'var(--color-border)',
          }}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.14em]" style={{ color: 'var(--color-primary)' }}>
                Command Center
              </p>
              <h2 className="mt-1 text-xl font-extrabold" style={{ color: 'var(--color-text)' }}>
                Comunicações
              </h2>
              <p className="mt-1 text-xs leading-relaxed" style={{ color: 'var(--color-text-subtle)' }}>
                Avisos, atualizações e pendências internas.
              </p>
            </div>
            <span
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl"
              style={{
                backgroundColor: 'color-mix(in srgb, var(--color-primary) 12%, var(--color-card))',
                color: 'var(--color-primary)',
              }}
            >
              <Bell size={18} />
            </span>
          </div>

          <div className="mt-4 grid grid-cols-3 gap-1 rounded-2xl border p-1" style={{ borderColor: 'var(--color-border)' }}>
            {(Object.keys(TAB_LABELS) as HomeCommunicationTab[]).map((tab) => {
              const active = activeTab === tab;
              return (
                <button
                  key={tab}
                  type="button"
                  onClick={() => handleTabChange(tab)}
                  className={`h-9 rounded-xl text-[11px] font-bold transition-all duration-200 ${focusRingClass}`}
                  style={{
                    backgroundColor: active ? 'var(--color-primary)' : 'transparent',
                    color: active ? '#FFFFFF' : 'var(--color-text-muted)',
                  }}
                  aria-pressed={active}
                >
                  {TAB_LABELS[tab]}
                </button>
              );
            })}
          </div>

          {isAdmin && (
            <button
              type="button"
              onClick={onStartCreate}
              className={`mt-4 inline-flex h-9 w-full items-center justify-center gap-2 rounded-xl px-3 text-xs font-bold text-white transition-all duration-200 hover:-translate-y-0.5 hover:opacity-95 ${focusRingClass}`}
              style={{ backgroundColor: 'var(--color-primary)' }}
            >
              <Plus size={14} />
              Novo comunicado
            </button>
          )}
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-5">
          {showForm && isAdmin && (
            <form
              onSubmit={onSubmit}
              className="mb-5 space-y-3 rounded-[22px] border p-4"
              style={{
                backgroundColor: 'color-mix(in srgb, var(--color-text) 4%, var(--color-card))',
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
                        ? 'color-mix(in srgb, var(--color-primary) 8%, var(--color-card))'
                        : 'var(--color-card)',
                      borderColor: selected ? 'color-mix(in srgb, var(--color-primary) 36%, var(--color-border))' : 'var(--color-border)',
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

        <div className="border-t px-5 py-4" style={{ borderColor: 'var(--color-border)' }}>
          <button
            type="button"
            onClick={() => setShowAll((current) => !current)}
            className={`inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl border text-xs font-bold transition-all duration-200 hover:bg-[var(--color-bg)] ${focusRingClass}`}
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            <CheckCircle2 size={15} />
            {showAll ? 'Mostrar menos' : 'Ver todas'}
          </button>
        </div>
      </section>
    </aside>
  );
}
