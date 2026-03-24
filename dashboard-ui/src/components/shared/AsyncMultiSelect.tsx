import { useEffect, useMemo, useRef, useState } from 'react';
import { Popover, PopoverContent, PopoverTrigger } from '../ui/popover';

interface AsyncMultiSelectProps {
  label: string;
  opcoes: string[];
  selecionados: string[];
  onChange: (valores: string[]) => void;
  placeholder?: string;
  isLoading?: boolean;
}

export default function AsyncMultiSelect({
  label,
  opcoes,
  selecionados,
  onChange,
  placeholder = 'Selecionar',
  isLoading,
}: AsyncMultiSelectProps) {
  const [aberto, setAberto] = useState(false);
  const [busca, setBusca] = useState('');

  // Cache de opções: evita lista vazia durante re-fetch em background
  const cachedRef = useRef<string[]>([]);
  useEffect(() => {
    if (opcoes.length > 0) cachedRef.current = opcoes;
  }, [opcoes]);
  const opcoesEfetivas = opcoes.length > 0 ? opcoes : cachedRef.current;

  function handleOpenChange(next: boolean) {
    setAberto(next);
    if (!next) setBusca(''); // reset busca ao fechar
  }

  const opcoesFiltradas = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    if (!termo) return opcoesEfetivas;
    return opcoesEfetivas.filter((o) => o.toLowerCase().includes(termo));
  }, [busca, opcoesEfetivas]);

  function alternar(valor: string) {
    if (selecionados.includes(valor)) {
      onChange(selecionados.filter((v) => v !== valor));
    } else {
      onChange([...selecionados, valor]);
    }
  }

  const temSelecao = selecionados.length > 0;

  return (
    <Popover open={aberto} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          className="min-w-[160px] cursor-pointer rounded-[14px] border px-3 py-2 text-left shadow-sm
                     transition-all duration-150 hover:border-[var(--color-primary)] active:scale-[0.97]"
          style={{
            backgroundColor: 'var(--color-card)',
            borderColor: aberto ? 'var(--color-primary)' : 'var(--color-border)',
            boxShadow: aberto ? '0 0 0 2px color-mix(in srgb, var(--color-primary) 15%, transparent)' : undefined,
          }}
        >
          <span
            className="mb-0.5 flex items-center gap-1.5 text-xs font-medium"
            style={{ color: 'var(--color-text-muted)' }}
          >
            {label}
            {temSelecao && (
              <span
                className="inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none text-white"
                style={{ backgroundColor: 'var(--color-primary)' }}
              >
                {selecionados.length}
              </span>
            )}
          </span>
          <span
            className="block truncate text-sm font-medium"
            style={{ color: temSelecao ? 'var(--color-text)' : 'var(--color-text-muted)' }}
          >
            {temSelecao ? `${selecionados.length} selecionado(s)` : placeholder}
          </span>
        </button>
      </PopoverTrigger>

      <PopoverContent>
        {/* Campo de busca */}
        <input
          type="search"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder={`Buscar ${label.toLowerCase()}…`}
          className="mb-3 w-full rounded-lg border px-3 py-2 text-sm outline-none
                     transition-all duration-150 focus:border-[var(--color-primary)]"
          style={{
            backgroundColor: 'var(--color-bg)',
            borderColor: 'var(--color-border)',
            color: 'var(--color-text)',
          }}
          autoFocus
        />

        {/* Linha de controle */}
        <div className="mb-2 flex items-center justify-between">
          <button
            type="button"
            onClick={() => onChange([])}
            className="cursor-pointer text-xs font-medium transition-opacity hover:opacity-70 active:scale-95"
            style={{ color: 'var(--color-primary)' }}
          >
            Limpar seleção
          </button>
          {isLoading && (
            <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              Carregando…
            </span>
          )}
          {!isLoading && temSelecao && (
            <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              {selecionados.length} selecionado(s)
            </span>
          )}
        </div>

        {/* Lista */}
        <div className="max-h-56 space-y-0.5 overflow-y-auto">
          {opcoesFiltradas.map((opcao) => {
            const selecionado = selecionados.includes(opcao);
            return (
              <label
                key={opcao}
                className="flex cursor-pointer items-center gap-2.5 rounded-md px-2 py-1.5 text-sm
                           transition-all duration-100"
                style={
                  selecionado
                    ? {
                        color: 'var(--color-primary)',
                        backgroundColor: 'color-mix(in srgb, var(--color-primary) 8%, transparent)',
                        fontWeight: 500,
                      }
                    : { color: 'var(--color-text)' }
                }
                onMouseEnter={(e) => {
                  if (!selecionado) (e.currentTarget as HTMLElement).style.backgroundColor = 'var(--color-bg)';
                }}
                onMouseLeave={(e) => {
                  if (!selecionado) (e.currentTarget as HTMLElement).style.backgroundColor = '';
                }}
              >
                <input
                  type="checkbox"
                  checked={selecionado}
                  onChange={() => alternar(opcao)}
                  className="shrink-0 cursor-pointer rounded"
                  style={{ accentColor: 'var(--color-primary)' }}
                />
                <span className="truncate">{opcao}</span>
              </label>
            );
          })}

          {!isLoading && opcoesFiltradas.length === 0 && (
            <p className="py-3 text-center text-xs" style={{ color: 'var(--color-text-muted)' }}>
              Nenhuma opção encontrada.
            </p>
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}
