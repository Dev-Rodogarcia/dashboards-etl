import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export interface ColunaTabela<T> {
  chave: keyof T & string;
  label: string;
  formato?: (valor: T[keyof T], row: T) => string | ReactNode;
  largura?: string;
  fixo?: boolean;
  ordenavel?: boolean;
}

type ItemPaginacao = number | 'ellipsis-start' | 'ellipsis-end';

interface DataTableProps<T> {
  dados: T[];
  colunas: ColunaTabela<T>[];
  chaveLinha: keyof T & string;
  isLoading?: boolean;
  titulo?: string;
  mostrarCabecalho?: boolean;
  paginaInicial?: number;
  tamanhoPaginaInicial?: number;
  totalRegistros?: number;
  paginaAtual?: number;
  tamanhoPagina?: number;
  onPaginaChange?: (pagina: number) => void;
  onTamanhoPaginaChange?: (tamanhoPagina: number) => void;
}

export default function DataTable<T>({
  dados,
  colunas,
  chaveLinha,
  isLoading,
  titulo,
  mostrarCabecalho = true,
  paginaInicial = 1,
  tamanhoPaginaInicial = 10,
  totalRegistros,
  paginaAtual: paginaAtualControlada,
  tamanhoPagina: tamanhoPaginaControlado,
  onPaginaChange,
  onTamanhoPaginaChange,
}: DataTableProps<T>) {
  const [ordenarPor, setOrdenarPor] = useState<string | null>(null);
  const [direcao, setDirecao] = useState<'asc' | 'desc'>('asc');
  const [paginaAtualInterna, setPaginaAtualInterna] = useState(paginaInicial);
  const [tamanhoPaginaInterno, setTamanhoPaginaInterno] = useState(tamanhoPaginaInicial);
  const colunaOrdenada = colunas.find((coluna) => coluna.chave === ordenarPor);
  const paginaAtual = paginaAtualControlada ?? paginaAtualInterna;
  const tamanhoPagina = tamanhoPaginaControlado ?? tamanhoPaginaInterno;
  const paginacaoRemota = Boolean(onPaginaChange || onTamanhoPaginaChange);

  function alterarPagina(proximaPagina: number) {
    if (onPaginaChange) {
      onPaginaChange(proximaPagina);
      return;
    }
    setPaginaAtualInterna(proximaPagina);
  }

  function alterarTamanhoPagina(proximoTamanho: number) {
    const primeiroRegistroAtual = dadosPaginados.length === 0 ? 1 : inicio + 1;
    const totalPaginasNovo = Math.max(1, Math.ceil(totalBasePaginacao / proximoTamanho));
    const proximaPagina = Math.min(
      totalPaginasNovo,
      Math.max(1, Math.ceil(primeiroRegistroAtual / proximoTamanho)),
    );

    if (onTamanhoPaginaChange) {
      onTamanhoPaginaChange(proximoTamanho);
    } else {
      setTamanhoPaginaInterno(proximoTamanho);
    }
    alterarPagina(proximaPagina);
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
    alterarPagina(1);
  }

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

  const totalReal = totalRegistros ?? dados.length;
  const totalBasePaginacao = paginacaoRemota ? totalReal : dadosOrdenados.length;
  const totalPaginas = Math.max(1, Math.ceil(totalBasePaginacao / tamanhoPagina));
  const paginaSegura = Math.min(paginaAtual, totalPaginas);
  const inicio = (paginaSegura - 1) * tamanhoPagina;
  const dadosPaginados = paginacaoRemota ? dadosOrdenados : dadosOrdenados.slice(inicio, inicio + tamanhoPagina);
  const fimExibido = paginacaoRemota
    ? Math.min(inicio + dadosPaginados.length, totalReal)
    : Math.min(inicio + dadosPaginados.length, dados.length);
  const resumoRegistros = paginacaoRemota
    ? `${totalReal} registros encontrados`
    : totalReal > dados.length
    ? `${dados.length} de ${totalReal} registros carregados`
    : `${dados.length} registros carregados`;
  const itensPaginacao = useMemo<ItemPaginacao[]>(() => {
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
  }, [paginaSegura, totalPaginas]);

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
      {mostrarCabecalho && (
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
          <label className="flex flex-wrap items-center gap-2 text-xs" style={{ color: 'var(--color-text-muted)' }}>
            Linhas
            <select
              value={tamanhoPagina}
              onChange={(event) => alterarTamanhoPagina(Number(event.target.value))}
              className="rounded-lg border px-2 py-1 text-xs"
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
      )}

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
                  } ${
                    col.fixo ? 'sticky left-0 z-10' : ''
                  }`}
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
          </thead>
          <tbody>
            {dadosPaginados.length === 0 ? (
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
              dadosPaginados.map((row) => (
                <tr
                  key={String(row[chaveLinha])}
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
          Mostrando {dadosPaginados.length === 0 ? 0 : inicio + 1} a{' '}
          {fimExibido} de {paginacaoRemota ? totalReal : dados.length}
        </span>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            onClick={() => alterarPagina(Math.max(1, paginaSegura - 1))}
            disabled={paginaSegura === 1}
            className="rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            Anterior
          </button>
          <div className="flex flex-wrap items-center justify-center gap-1" aria-label="Páginas da tabela">
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
                  onClick={() => alterarPagina(item)}
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
            onClick={() => alterarPagina(Math.min(totalPaginas, paginaSegura + 1))}
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
