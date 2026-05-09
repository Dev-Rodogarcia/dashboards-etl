import { useId, useMemo, useState } from 'react';
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
  const labelId = useId();
  const valueId = useId();
  const opcoesEfetivas = opcoes;

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
      <div className="flex w-full min-w-[148px] flex-col gap-1 self-start justify-self-start md:w-auto">
        <span
          id={labelId}
          className="flex min-h-4 items-center gap-1.5 text-xs font-medium leading-4"
          style={{ color: 'var(--color-text-muted)' }}
        >
          {label}
          {temSelecao && (
            <span
              className="inline-flex items-center rounded-full border px-1.5 py-0.5 text-[10px] font-bold leading-none"
              style={{ backgroundColor: 'rgba(33, 71, 138, 0.14)', borderColor: 'var(--color-primary)', color: 'var(--color-primary)' }}
            >
              {selecionados.length}
            </span>
          )}
        </span>

        <PopoverTrigger asChild>
          <button
            type="button"
            aria-labelledby={`${labelId} ${valueId}`}
            className="h-10 w-full cursor-pointer rounded-lg border px-3 text-left text-sm shadow-sm
                       transition-all duration-150 hover:border-[var(--color-primary)] active:scale-[0.97]
                       focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{
              backgroundColor: 'var(--color-card)',
              borderColor: aberto ? 'var(--color-primary)' : 'var(--color-border)',
              boxShadow: aberto ? '0 0 0 2px var(--color-primary)' : undefined,
            }}
          >
            <span
              id={valueId}
              className="block truncate font-medium leading-none"
              style={{ color: temSelecao ? 'var(--color-text)' : 'var(--color-text-muted)' }}
            >
              {temSelecao ? `${selecionados.length} selecionado(s)` : placeholder}
            </span>
          </button>
        </PopoverTrigger>
      </div>

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
                        backgroundColor: 'var(--color-bg)',
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
