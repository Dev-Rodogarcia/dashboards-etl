import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import DataTable from './DataTable';

describe('DataTable', () => {
  it('exibe total real quando maior que linhas carregadas', () => {
    type Row = { id: number; nome: string };
    const Tabela = DataTable<Row>;
    const html = renderToStaticMarkup(
      createElement(Tabela, {
        titulo: 'Fretes',
        dados: [{ id: 1, nome: 'A' }, { id: 2, nome: 'B' }],
        colunas: [{ chave: 'nome', label: 'Nome' }],
        chaveLinha: 'id',
        totalRegistros: 4944,
      }),
    );

    expect(html).toContain('2 de 4944 registros carregados');
  });

  it('usa total real no rodape quando a paginacao e remota', () => {
    type Row = { id: number; nome: string };
    const Tabela = DataTable<Row>;
    const html = renderToStaticMarkup(
      createElement(Tabela, {
        titulo: 'Fretes',
        dados: [{ id: 11, nome: 'K' }, { id: 12, nome: 'L' }],
        colunas: [{ chave: 'nome', label: 'Nome' }],
        chaveLinha: 'id',
        totalRegistros: 4944,
        paginaAtual: 2,
        tamanhoPagina: 10,
        onPaginaChange: () => undefined,
      }),
    );

    expect(html).toContain('4944 registros encontrados');
    expect(html).toContain('Mostrando 11 a 12 de 4944');
  });

  it('mostra atalhos numericos de pagina com reticencias', () => {
    type Row = { id: number; nome: string };
    const Tabela = DataTable<Row>;
    const html = renderToStaticMarkup(
      createElement(Tabela, {
        titulo: 'Fretes',
        dados: [{ id: 91, nome: 'A' }],
        colunas: [{ chave: 'nome', label: 'Nome' }],
        chaveLinha: 'id',
        totalRegistros: 5040,
        paginaAtual: 10,
        tamanhoPagina: 10,
        onPaginaChange: () => undefined,
      }),
    );

    expect(html).toContain('Pagina 10 de 504');
    expect(html).toContain('aria-current="page"');
    expect(html).toContain('...');
    expect(html).toContain('504');
  });
});
