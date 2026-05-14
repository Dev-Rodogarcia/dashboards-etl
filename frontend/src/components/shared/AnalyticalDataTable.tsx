import { useEffect, useId, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Funnel, Search, SlidersHorizontal, X } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '../ui/popover';
import type { ColunaTabela } from './DataTable';
import type { TableColumnFilterValue, TableFilterField, TableFilters } from '../../types/tableFilters';

export type ColunaTabelaAnalitica<T> = ColunaTabela<T> & {
  filtroTabela?: TableFilterField;
};

type ItemPaginacao = number | 'ellipsis-start' | 'ellipsis-end';

interface AnalyticalDataTableProps<T> {
  dados: T[];
  colunas: ColunaTabelaAnalitica<T>[];
  chaveLinha: keyof T & string;
  filtros: TableFilters;
  hiddenActiveCount: number;
  hasAnyFilter: boolean;
  onTextFilterChange: (campo: Exclude<keyof TableFilters, 'status' | 'columnFilters'>, valor: string) => void;
  onMultiFilterChange: (campo: Extract<keyof TableFilters, 'status'>, valores: string[]) => void;
  onColumnFilterChange: (chaveColuna: string, valor: string | string[]) => void;
  onClearFilters: () => void;
  statusOptions?: string[];
  isLoading?: boolean;
  titulo?: string;
  totalRegistros?: number;
  paginaAtual: number;
  tamanhoPagina: number;
  onPaginaChange: (pagina: number) => void;
  onTamanhoPaginaChange: (tamanhoPagina: number) => void;
}

function useIsMobile() {
  const [mobile, setMobile] = useState(() =>
    typeof window !== 'undefined' ? window.innerWidth < 768 : false,
  );

  useEffect(() => {
    const fn = () => setMobile(window.innerWidth < 768);
    window.addEventListener('resize', fn);
    return () => window.removeEventListener('resize', fn);
  }, []);

  return mobile;
}

function toggleValue(valores: string[], valor: string): string[] {
  return valores.includes(valor)
    ? valores.filter((item) => item !== valor)
    : [...valores, valor];
}

function uniqueOptions(opcoes: string[] | undefined): string[] {
  return Array.from(new Set((opcoes ?? []).filter((item) => item && item.trim().length > 0))).sort((a, b) =>
    a.localeCompare(b, 'pt-BR'),
  );
}

function TextFilter({
  value,
  onChange,
  placeholder,
  compact = false,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  compact?: boolean;
}) {
  return (
    <div className="relative">
      <Search
        size={13}
        className="pointer-events-none absolute left-2 top-1/2 -translate-y-1/2"
        style={{ color: 'var(--color-text-muted)' }}
        aria-hidden="true"
      />
      <input
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className={`w-full rounded-lg border pl-7 pr-2 text-xs outline-none transition-colors focus:border-[var(--color-primary)] ${
          compact ? 'h-8 min-w-[132px]' : 'h-9'
        }`}
        style={{
          backgroundColor: 'var(--color-bg)',
          borderColor: 'var(--color-border)',
          color: 'var(--color-text)',
        }}
      />
    </div>
  );
}

function MultiSelectFilter({
  label,
  opcoes,
  selecionados,
  onChange,
  compact = false,
}: {
  label: string;
  opcoes: string[];
  selecionados: string[];
  onChange: (valores: string[]) => void;
  compact?: boolean;
}) {
  const labelId = useId();
  const opcoesEfetivas = uniqueOptions(opcoes);

  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          id={labelId}
          className={`flex w-full items-center justify-between gap-2 rounded-lg border px-2 text-left text-xs transition-colors hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] ${
            compact ? 'h-8 min-w-[132px]' : 'h-9'
          }`}
          style={{
            backgroundColor: 'var(--color-bg)',
            borderColor: 'var(--color-border)',
            color: selecionados.length > 0 ? 'var(--color-text)' : 'var(--color-text-muted)',
          }}
        >
          <span className="flex min-w-0 items-center gap-1.5">
            <Funnel size={12} aria-hidden="true" />
            <span className="truncate">{selecionados.length > 0 ? `${selecionados.length} selecionado(s)` : label}</span>
          </span>
        </button>
      </PopoverTrigger>
      <PopoverContent aria-labelledby={labelId} style={{ minWidth: 240 }}>
        <div className="mb-2 flex items-center justify-between gap-2">
          <span className="text-xs font-medium" style={{ color: 'var(--color-text)' }}>
            {label}
          </span>
          <button
            type="button"
            onClick={() => onChange([])}
            className="text-xs font-medium transition-opacity hover:opacity-70"
            style={{ color: 'var(--color-primary)' }}
          >
            Limpar
          </button>
        </div>
        <div className="max-h-56 space-y-0.5 overflow-y-auto">
          {opcoesEfetivas.map((opcao) => {
            const selecionado = selecionados.includes(opcao);
            return (
              <label
                key={opcao}
                className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm transition-colors hover:bg-[var(--color-bg)]"
                style={{ color: selecionado ? 'var(--color-primary)' : 'var(--color-text)' }}
              >
                <input
                  type="checkbox"
                  checked={selecionado}
                  onChange={() => onChange(toggleValue(selecionados, opcao))}
                  className="shrink-0 cursor-pointer rounded"
                  style={{ accentColor: 'var(--color-primary)' }}
                />
                <span className="truncate">{opcao}</span>
              </label>
            );
          })}
          {opcoesEfetivas.length === 0 && (
            <p className="py-3 text-center text-xs" style={{ color: 'var(--color-text-muted)' }}>
              Nenhuma opcao disponivel.
            </p>
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}

function ColumnFilterControl({
  chaveColuna,
  label,
  isStatus,
  filtros,
  statusOptions,
  onColumnFilterChange,
}: {
  chaveColuna: string;
  label: string;
  isStatus: boolean;
  filtros: TableFilters;
  statusOptions: string[];
  onColumnFilterChange: (chaveColuna: string, valor: string | string[]) => void;
}) {
  const valorAtual = filtros.columnFilters?.[chaveColuna];

  if (isStatus) {
    return (
      <MultiSelectFilter
        compact
        label={label}
        opcoes={statusOptions}
        selecionados={normalizarValorMulti(valorAtual)}
        onChange={(valores) => onColumnFilterChange(chaveColuna, valores)}
      />
    );
  }

  return (
    <TextFilter
      compact
      value={normalizarValorTexto(valorAtual)}
      onChange={(valor) => onColumnFilterChange(chaveColuna, valor)}
      placeholder={label}
    />
  );
}

function normalizarValorTexto(valor: TableColumnFilterValue | undefined): string {
  if (Array.isArray(valor)) {
    return valor[0] ?? '';
  }
  return valor ?? '';
}

function normalizarValorMulti(valor: TableColumnFilterValue | undefined): string[] {
  if (Array.isArray(valor)) {
    return valor;
  }
  return valor ? [valor] : [];
}

function AdvancedFiltersContent({
  campos,
  filtros,
  statusOptions,
  onTextFilterChange,
  onMultiFilterChange,
}: {
  campos: TableFilterField[];
  filtros: TableFilters;
  statusOptions: string[];
  onTextFilterChange: (campo: Exclude<keyof TableFilters, 'status' | 'columnFilters'>, valor: string) => void;
  onMultiFilterChange: (campo: Extract<keyof TableFilters, 'status'>, valores: string[]) => void;
}) {
  return (
    <div className="grid gap-3">
      {campos.includes('codigo') && (
        <label className="grid gap-1 text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Codigo
          <TextFilter value={filtros.codigo ?? ''} onChange={(valor) => onTextFilterChange('codigo', valor)} placeholder="Codigo" />
        </label>
      )}
      {campos.includes('placa') && (
        <label className="grid gap-1 text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Placa
          <TextFilter value={filtros.placa ?? ''} onChange={(valor) => onTextFilterChange('placa', valor)} placeholder="Placa" />
        </label>
      )}
      {campos.includes('status') && (
        <div className="grid gap-1 text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Status
          <MultiSelectFilter
            label="Status"
            opcoes={statusOptions}
            selecionados={filtros.status ?? []}
            onChange={(valores) => onMultiFilterChange('status', valores)}
          />
        </div>
      )}
      {campos.includes('razaoSocial') && (
        <label className="grid gap-1 text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Razao Social
          <TextFilter
            value={filtros.razaoSocial ?? ''}
            onChange={(valor) => onTextFilterChange('razaoSocial', valor)}
            placeholder="Razao social"
          />
        </label>
      )}
      {campos.includes('origem') && (
        <label className="grid gap-1 text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Origem
          <TextFilter value={filtros.origem ?? ''} onChange={(valor) => onTextFilterChange('origem', valor)} placeholder="Origem" />
        </label>
      )}
      {campos.includes('destino') && (
        <label className="grid gap-1 text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Destino
          <TextFilter value={filtros.destino ?? ''} onChange={(valor) => onTextFilterChange('destino', valor)} placeholder="Destino" />
        </label>
      )}
    </div>
  );
}

function AdvancedFiltersButton<T>({
  colunas,
  filtros,
  hiddenActiveCount,
  statusOptions,
  onTextFilterChange,
  onMultiFilterChange,
}: Pick<
  AnalyticalDataTableProps<T>,
  'colunas' | 'filtros' | 'hiddenActiveCount' | 'statusOptions' | 'onTextFilterChange' | 'onMultiFilterChange'
>) {
  const [open, setOpen] = useState(false);
  const isMobile = useIsMobile();
  const campos = useMemo(
    () => Array.from(new Set(colunas.map((coluna) => coluna.filtroTabela).filter(Boolean))) as TableFilterField[],
    [colunas],
  );
  const statusOptionsEfetivas = statusOptions ?? [];

  const content = (
    <AdvancedFiltersContent
      campos={campos}
      filtros={filtros}
      statusOptions={statusOptionsEfetivas}
      onTextFilterChange={onTextFilterChange}
      onMultiFilterChange={onMultiFilterChange}
    />
  );

  const renderButton = (withManualToggle: boolean) => (
    <button
      type="button"
      onClick={withManualToggle ? () => setOpen((atual) => !atual) : undefined}
      className="relative inline-flex h-9 items-center gap-1.5 rounded-lg border px-3 text-xs font-medium transition-colors hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
      style={{
        backgroundColor: 'var(--color-bg)',
        borderColor: open ? 'var(--color-primary)' : 'var(--color-border)',
        color: 'var(--color-text)',
      }}
      aria-expanded={open}
    >
      <SlidersHorizontal size={14} aria-hidden="true" />
      Filtros
      {hiddenActiveCount > 0 && (
        <span
          className="inline-flex min-w-5 items-center justify-center rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none"
          style={{ backgroundColor: 'rgba(33, 71, 138, 0.14)', color: 'var(--color-primary)' }}
          aria-label={`${hiddenActiveCount} filtros ativos`}
        >
          {hiddenActiveCount}
        </span>
      )}
    </button>
  );

  if (isMobile) {
    return (
      <>
        {renderButton(true)}
        {typeof document !== 'undefined'
          ? createPortal(
              open ? (
                <>
                  <div className="fixed inset-0 z-40 bg-black/40" onClick={() => setOpen(false)} aria-hidden="true" />
                  <div
                    role="dialog"
                    aria-modal="true"
                    aria-label="Filtros da tabela"
                    className="fixed bottom-0 left-0 right-0 z-50 rounded-t-[24px] border-t p-5 shadow-2xl"
                    style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
                  >
                    <div className="mb-4 flex items-center justify-between gap-3">
                      <span className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
                        Filtros da tabela
                      </span>
                      <button
                        type="button"
                        onClick={() => setOpen(false)}
                        className="rounded-full p-1.5 transition-colors hover:bg-[var(--color-bg)]"
                        style={{ color: 'var(--color-text-muted)' }}
                        aria-label="Fechar filtros"
                      >
                        <X size={16} />
                      </button>
                    </div>
                    <div className="max-h-[70vh] overflow-y-auto pb-2">{content}</div>
                  </div>
                </>
              ) : null,
              document.body,
            )
          : null}
      </>
    );
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>{renderButton(false)}</PopoverTrigger>
      <PopoverContent align="end" style={{ width: 340, minWidth: 340 }}>
        {content}
      </PopoverContent>
    </Popover>
  );
}

function buildPaginationItems(paginaSegura: number, totalPaginas: number): ItemPaginacao[] {
  if (totalPaginas <= 7) {
    return Array.from({ length: totalPaginas }, (_, index) => index + 1);
  }

  if (paginaSegura <= 4) {
    return [1, 2, 3, 4, 5, 'ellipsis-end', totalPaginas];
  }

  if (paginaSegura >= totalPaginas - 3) {
    return [1, 'ellipsis-start', totalPaginas - 4, totalPaginas - 3, totalPaginas - 2, totalPaginas - 1, totalPaginas];
  }

  return [1, 'ellipsis-start', paginaSegura - 2, paginaSegura - 1, paginaSegura, paginaSegura + 1, paginaSegura + 2, 'ellipsis-end', totalPaginas];
}

export default function AnalyticalDataTable<T>({
  dados,
  colunas,
  chaveLinha,
  filtros,
  hiddenActiveCount,
  hasAnyFilter,
  onTextFilterChange,
  onMultiFilterChange,
  onColumnFilterChange,
  onClearFilters,
  statusOptions,
  isLoading,
  titulo,
  totalRegistros,
  paginaAtual,
  tamanhoPagina,
  onPaginaChange,
  onTamanhoPaginaChange,
}: AnalyticalDataTableProps<T>) {
  const [ordenarPor, setOrdenarPor] = useState<string | null>(null);
  const [direcao, setDirecao] = useState<'asc' | 'desc'>('asc');
  const colunaOrdenada = colunas.find((coluna) => coluna.chave === ordenarPor);
  const totalReal = totalRegistros ?? dados.length;
  const totalPaginas = Math.max(1, Math.ceil(totalReal / tamanhoPagina));
  const paginaSegura = Math.min(paginaAtual, totalPaginas);
  const inicio = (paginaSegura - 1) * tamanhoPagina;
  const fimExibido = Math.min(inicio + dados.length, totalReal);
  const resumoRegistros = `${totalReal} registros encontrados`;
  const statusOptionsEfetivas = statusOptions ?? [];

  const dadosOrdenados = useMemo(() => {
    if (!ordenarPor || !colunaOrdenada || colunaOrdenada.ordenavel === false) {
      return dados;
    }

    return [...dados].sort((a, b) => {
      const va = a[ordenarPor as keyof T];
      const vb = b[ordenarPor as keyof T];

      if (va == null && vb == null) return 0;
      if (va == null) return 1;
      if (vb == null) return -1;

      const valorA = typeof va === 'number' ? va : String(va).toLowerCase();
      const valorB = typeof vb === 'number' ? vb : String(vb).toLowerCase();

      if (valorA < valorB) return direcao === 'asc' ? -1 : 1;
      if (valorA > valorB) return direcao === 'asc' ? 1 : -1;
      return 0;
    });
  }, [colunaOrdenada, dados, direcao, ordenarPor]);

  const itensPaginacao = useMemo(() => buildPaginationItems(paginaSegura, totalPaginas), [paginaSegura, totalPaginas]);

  function alterarTamanhoPagina(proximoTamanho: number) {
    const primeiroRegistroAtual = dados.length === 0 ? 1 : inicio + 1;
    const totalPaginasNovo = Math.max(1, Math.ceil(totalReal / proximoTamanho));
    const proximaPagina = Math.min(totalPaginasNovo, Math.max(1, Math.ceil(primeiroRegistroAtual / proximoTamanho)));
    onTamanhoPaginaChange(proximoTamanho);
    onPaginaChange(proximaPagina);
  }

  function handleSort(chave: string, ordenavel = true) {
    if (!ordenavel) {
      return;
    }

    if (ordenarPor === chave) {
      setDirecao((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setOrdenarPor(chave);
      setDirecao('asc');
    }
    onPaginaChange(1);
  }

  if (isLoading) {
    return (
      <div
        className="flex items-center justify-center rounded-[20px] border p-8 shadow-sm"
        style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
      >
        <div
          className="h-6 w-6 animate-spin rounded-full border-2 border-t-transparent"
          style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }}
        />
      </div>
    );
  }

  return (
    <div
      className="overflow-hidden rounded-[20px] border shadow-sm"
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
    >
      <div
        className="flex flex-wrap items-center justify-between gap-3 border-b px-4 py-3"
        style={{ borderColor: 'var(--color-border)' }}
      >
        <div>
          <h3 className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
            {titulo ?? 'Tabela analitica'}
          </h3>
          <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
            {resumoRegistros}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="min-w-[210px] flex-1 sm:flex-none">
            <TextFilter
              value={filtros.busca ?? ''}
              onChange={(valor) => onTextFilterChange('busca', valor)}
              placeholder="Buscar na tabela..."
            />
          </div>
          <AdvancedFiltersButton
            colunas={colunas}
            filtros={filtros}
            hiddenActiveCount={hiddenActiveCount}
            statusOptions={statusOptionsEfetivas}
            onTextFilterChange={onTextFilterChange}
            onMultiFilterChange={onMultiFilterChange}
          />
          <button
            type="button"
            onClick={onClearFilters}
            disabled={!hasAnyFilter}
            className="inline-flex h-9 items-center gap-1.5 rounded-lg border px-3 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)',
            }}
          >
            Limpar
          </button>
          <label className="flex flex-wrap items-center gap-2 text-xs" style={{ color: 'var(--color-text-muted)' }}>
            Linhas
            <select
              value={tamanhoPagina}
              onChange={(event) => alterarTamanhoPagina(Number(event.target.value))}
              className="h-9 rounded-lg border px-2 py-1 text-xs"
              style={{
                backgroundColor: 'var(--color-bg)',
                borderColor: 'var(--color-border)',
                color: 'var(--color-text)',
              }}
            >
              {[10, 20, 50, 100].map((valor) => (
                <option key={valor} value={valor}>
                  {valor}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-full text-sm">
          <thead>
            <tr className="border-b" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
              {colunas.map((col) => (
                <th
                  key={col.chave}
                  onClick={() => handleSort(col.chave, col.ordenavel !== false)}
                  className={`px-3 py-2.5 text-left text-xs font-medium uppercase tracking-wider select-none ${
                    col.ordenavel === false ? 'cursor-default' : 'cursor-pointer'
                  } ${col.fixo ? 'sticky left-0 z-10' : ''}`}
                  style={{
                    color: 'var(--color-text-muted)',
                    backgroundColor: col.fixo ? 'var(--color-bg)' : undefined,
                    ...(col.largura ? { width: col.largura } : {}),
                  }}
                >
                  {col.label}
                  {col.ordenavel !== false && ordenarPor === col.chave && (
                    <span className="ml-1">{direcao === 'asc' ? '↑' : '↓'}</span>
                  )}
                </th>
              ))}
            </tr>
            <tr className="border-b" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
              {colunas.map((col) => (
                <th
                  key={`${col.chave}-filter`}
                  className={`px-3 py-2 align-top ${col.fixo ? 'sticky left-0 z-10' : ''}`}
                  style={{
                    backgroundColor: col.fixo ? 'var(--color-card)' : undefined,
                    ...(col.largura ? { width: col.largura } : {}),
                  }}
                >
                  <ColumnFilterControl
                    chaveColuna={col.chave}
                    label={col.label}
                    isStatus={col.filtroTabela === 'status'}
                    filtros={filtros}
                    statusOptions={statusOptionsEfetivas}
                    onColumnFilterChange={onColumnFilterChange}
                  />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {dadosOrdenados.length === 0 ? (
              <tr>
                <td
                  colSpan={colunas.length}
                  className="px-3 py-8 text-center"
                  style={{ color: 'var(--color-text-muted)' }}
                >
                  Nenhum registro encontrado.
                </td>
              </tr>
            ) : (
              dadosOrdenados.map((row, index) => (
                <tr
                  key={`${String(row[chaveLinha])}-${index}`}
                  className="border-b transition-colors"
                  style={{ borderColor: 'var(--color-border)' }}
                  onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.backgroundColor = 'var(--color-bg)'; }}
                  onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.backgroundColor = ''; }}
                >
                  {colunas.map((col) => (
                    <td
                      key={col.chave}
                      className={`px-3 py-2 whitespace-nowrap ${col.fixo ? 'sticky left-0' : ''}`}
                      style={{
                        color: 'var(--color-text)',
                        backgroundColor: col.fixo ? 'var(--color-card)' : undefined,
                        fontWeight: col.fixo ? 500 : undefined,
                      }}
                    >
                      {col.formato
                        ? col.formato(row[col.chave], row)
                        : String(row[col.chave] ?? '—')}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div
        className="flex flex-wrap items-center justify-between gap-3 border-t px-4 py-3 text-xs"
        style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
      >
        <span>
          Mostrando {dados.length === 0 ? 0 : inicio + 1} a {fimExibido} de {totalReal}
        </span>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            onClick={() => onPaginaChange(Math.max(1, paginaSegura - 1))}
            disabled={paginaSegura === 1}
            className="rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            Anterior
          </button>
          <div className="flex flex-wrap items-center justify-center gap-1" aria-label="Paginas da tabela">
            {itensPaginacao.map((item) => {
              if (typeof item !== 'number') {
                return (
                  <span
                    key={item}
                    className="px-1.5 text-xs"
                    style={{ color: 'var(--color-text-muted)' }}
                    aria-hidden="true"
                  >
                    ...
                  </span>
                );
              }

              const ativo = item === paginaSegura;
              return (
                <button
                  key={item}
                  type="button"
                  onClick={() => onPaginaChange(item)}
                  aria-current={ativo ? 'page' : undefined}
                  className="min-w-8 rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed"
                  style={
                    ativo
                      ? {
                          backgroundColor: 'var(--color-primary)',
                          borderColor: 'var(--color-primary)',
                          color: 'white',
                        }
                      : {
                          borderColor: 'var(--color-border)',
                          color: 'var(--color-text)',
                        }
                  }
                >
                  {item}
                </button>
              );
            })}
          </div>
          <span className="min-w-[92px] text-center">
            Pagina {paginaSegura} de {totalPaginas}
          </span>
          <button
            type="button"
            onClick={() => onPaginaChange(Math.min(totalPaginas, paginaSegura + 1))}
            disabled={paginaSegura === totalPaginas}
            className="rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            Proxima
          </button>
        </div>
      </div>
    </div>
  );
}
