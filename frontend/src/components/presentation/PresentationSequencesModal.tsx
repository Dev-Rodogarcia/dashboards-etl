import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, ChevronUp, Play, Plus, Trash2, X } from 'lucide-react';
import { criarApresentacao, excluirApresentacao, listarApresentacoes } from '../../api/endpoints/apresentacoesServico';
import type { ApresentacaoSequencia } from '../../types/apresentacao';

export interface PresentationPageOption { id: string; label: string; }

interface PresentationSequencesModalProps {
  open: boolean;
  paginasDisponiveis: PresentationPageOption[];
  onClose: () => void;
  onStart: (paginas: string[]) => void;
}

export default function PresentationSequencesModal({ open, paginasDisponiveis, onClose, onStart }: PresentationSequencesModalProps) {
  const [sequencias, setSequencias] = useState<ApresentacaoSequencia[]>([]);
  const [nome, setNome] = useState('Nova apresentação');
  const [paginas, setPaginas] = useState<string[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const paginasPorId = useMemo(() => new Map(paginasDisponiveis.map((pagina) => [pagina.id, pagina])), [paginasDisponiveis]);

  useEffect(() => {
    if (!open) return;
    setCarregando(true);
    void listarApresentacoes().then(setSequencias).catch(() => setSequencias([])).finally(() => setCarregando(false));
  }, [open]);

  if (!open) return null;

  const alternarPagina = (id: string) => setPaginas((atual) => atual.includes(id) ? atual.filter((pagina) => pagina !== id) : [...atual, id]);
  const mover = (indice: number, direcao: -1 | 1) => setPaginas((atual) => {
    const destino = indice + direcao;
    if (destino < 0 || destino >= atual.length) return atual;
    const proxima = [...atual]; [proxima[indice], proxima[destino]] = [proxima[destino], proxima[indice]]; return proxima;
  });
  const salvar = async () => {
    if (!nome.trim() || paginas.length === 0) return;
    setSalvando(true);
    try {
      const sequencia = await criarApresentacao({ nome: nome.trim(), paginas });
      setSequencias((atual) => [sequencia, ...atual]); setNome('Nova apresentação'); setPaginas([]);
    } finally { setSalvando(false); }
  };
  const excluir = async (id: number) => { await excluirApresentacao(id); setSequencias((atual) => atual.filter((sequencia) => sequencia.id !== id)); };

  return createPortal(
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/50 p-4" role="presentation" onMouseDown={onClose}>
      <section className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border p-5 shadow-2xl" role="dialog" aria-modal="true" aria-labelledby="presentation-sequences-title" onMouseDown={(event) => event.stopPropagation()} style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
        <div className="flex items-start justify-between gap-4"><div><h2 id="presentation-sequences-title" className="text-lg font-bold">Minhas apresentações</h2><p className="mt-1 text-sm" style={{ color: 'var(--color-text-muted)' }}>A sequência padrão permanece fixa; crie versões pessoais com as telas que você pode acessar.</p></div><button type="button" onClick={onClose} aria-label="Fechar" className="rounded-lg p-2"><X size={20} /></button></div>
        <div className="mt-6 grid gap-5 lg:grid-cols-2">
          <div className="rounded-xl border p-4" style={{ borderColor: 'var(--color-border)' }}>
            <h3 className="font-semibold">Criar versão</h3>
            <input value={nome} onChange={(event) => setNome(event.target.value)} maxLength={80} className="mt-3 h-10 w-full rounded-lg border px-3" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-bg)' }} aria-label="Nome da apresentação" />
            <div className="mt-3 space-y-2">{paginasDisponiveis.map((pagina) => <label key={pagina.id} className="flex items-center gap-2 text-sm"><input type="checkbox" checked={paginas.includes(pagina.id)} onChange={() => alternarPagina(pagina.id)} />{pagina.label}</label>)}</div>
            {paginas.length > 0 && <div className="mt-4 border-t pt-3" style={{ borderColor: 'var(--color-border)' }}><p className="text-xs font-semibold uppercase" style={{ color: 'var(--color-text-muted)' }}>Ordem</p>{paginas.map((id, indice) => <div key={id} className="mt-2 flex items-center justify-between gap-2 text-sm"><span>{indice + 1}. {paginasPorId.get(id)?.label}</span><span><button type="button" onClick={() => mover(indice, -1)} disabled={indice === 0} className="p-1 disabled:opacity-30" aria-label="Subir"><ChevronUp size={16} /></button><button type="button" onClick={() => mover(indice, 1)} disabled={indice === paginas.length - 1} className="p-1 disabled:opacity-30" aria-label="Descer"><ChevronDown size={16} /></button></span></div>)}</div>}
            <button type="button" onClick={() => void salvar()} disabled={salvando || !nome.trim() || paginas.length === 0} className="mt-4 inline-flex h-10 items-center gap-2 rounded-lg px-3 text-sm font-semibold text-white disabled:opacity-40" style={{ backgroundColor: 'var(--color-primary)' }}><Plus size={16} />Salvar versão</button>
          </div>
          <div className="rounded-xl border p-4" style={{ borderColor: 'var(--color-border)' }}><h3 className="font-semibold">Versões salvas</h3>{carregando ? <p className="mt-3 text-sm">Carregando…</p> : sequencias.length === 0 ? <p className="mt-3 text-sm" style={{ color: 'var(--color-text-muted)' }}>Nenhuma versão criada.</p> : <div className="mt-3 space-y-2">{sequencias.map((sequencia) => <div key={sequencia.id} className="rounded-lg border p-3" style={{ borderColor: 'var(--color-border)' }}><p className="font-semibold">{sequencia.nome}</p><p className="mt-1 text-xs" style={{ color: 'var(--color-text-muted)' }}>{sequencia.paginas.map((id) => paginasPorId.get(id)?.label).filter(Boolean).join(' → ') || 'Sem páginas disponíveis'}</p><div className="mt-3 flex gap-2"><button type="button" onClick={() => { onStart(sequencia.paginas.filter((id) => paginasPorId.has(id))); onClose(); }} disabled={sequencia.paginas.every((id) => !paginasPorId.has(id))} className="inline-flex items-center gap-1 text-sm font-semibold" style={{ color: 'var(--color-primary)' }}><Play size={15} />Iniciar</button><button type="button" onClick={() => void excluir(sequencia.id)} className="inline-flex items-center gap-1 text-sm text-red-500"><Trash2 size={15} />Excluir</button></div></div>)}</div>}</div>
        </div>
      </section>
    </div>, document.body);
}
