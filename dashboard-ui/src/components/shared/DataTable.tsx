import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export interface ColunaTabela<T> {
  chave: keyof T & string;
  label: string;
  formato?: (valor: T[keyof T], row: T) => string | ReactNode;
  largura?: string;
  fixo?: boolean;
}

interface DataTableProps<T> {
  dados: T[];
  colunas: ColunaTabela<T>[];
  chaveLinha: keyof T & string;
  isLoading?: boolean;
  titulo?: string;
  paginaInicial?: number;
  tamanhoPaginaInicial?: number;
}

export default function DataTable<T>({
  dados,
  colunas,
  chaveLinha,
  isLoading,
  titulo,
  paginaInicial = 1,
  tamanhoPaginaInicial = 10,
}: DataTableProps<T>) {
  const [ordenarPor, setOrdenarPor] = useState<string | null>(null);
  const [direcao, setDirecao] = useState<'asc' | 'desc'>('asc');
  const [paginaAtual, setPaginaAtual] = useState(paginaInicial);
  const [tamanhoPagina, setTamanhoPagina] = useState(tamanhoPaginaInicial);

  function handleSort(chave: string) {
    if (ordenarPor === chave) {
      setDirecao((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setOrdenarPor(chave);
      setDirecao('asc');
    }
    setPaginaAtual(1);
  }

  const dadosOrdenados = useMemo(() => {
    if (!ordenarPor) {
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
  }, [dados, direcao, ordenarPor]);

  const totalPaginas = Math.max(1, Math.ceil(dadosOrdenados.length / tamanhoPagina));
  const paginaSegura = Math.min(paginaAtual, totalPaginas);
  const inicio = (paginaSegura - 1) * tamanhoPagina;
  const dadosPaginados = dadosOrdenados.slice(inicio, inicio + tamanhoPagina);

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
            {dados.length} registros carregados
          </p>
        </div>
        <label className="flex items-center gap-2 text-xs" style={{ color: 'var(--color-text-muted)' }}>
          Linhas
          <select
            value={tamanhoPagina}
            onChange={(event) => {
              setTamanhoPagina(Number(event.target.value));
              setPaginaAtual(1);
            }}
            className="rounded-lg border px-2 py-1 text-xs"
            style={{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)',
            }}
          >
            {[10, 20, 50].map((valor) => (
              <option key={valor} value={valor}>
                {valor}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-full text-sm">
          <thead>
            <tr className="border-b" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
              {colunas.map((col) => (
                <th
                  key={col.chave}
                  onClick={() => handleSort(col.chave)}
                  className={`px-3 py-2.5 text-left text-xs font-medium uppercase tracking-wider cursor-pointer ${
                    col.fixo ? 'sticky left-0 z-10' : ''
                  }`}
                  style={{
                    color: 'var(--color-text-muted)',
                    backgroundColor: col.fixo ? 'var(--color-bg)' : undefined,
                    ...(col.largura ? { width: col.largura } : {}),
                  }}
                >
                  {col.label}
                  {ordenarPor === col.chave && (
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
        className="flex items-center justify-between border-t px-4 py-3 text-xs"
        style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
      >
        <span>
          Mostrando {dadosPaginados.length === 0 ? 0 : inicio + 1} a{' '}
          {Math.min(inicio + dadosPaginados.length, dados.length)} de {dados.length}
        </span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setPaginaAtual((pagina) => Math.max(1, pagina - 1))}
            disabled={paginaSegura === 1}
            className="rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            Anterior
          </button>
          <span>
            Pagina {paginaSegura} de {totalPaginas}
          </span>
          <button
            type="button"
            onClick={() => setPaginaAtual((pagina) => Math.min(totalPaginas, pagina + 1))}
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
