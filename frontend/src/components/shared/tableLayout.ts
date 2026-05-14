import type { CSSProperties } from 'react';

const LARGURA_COLUNA_PADRAO_PX = 150;
const LARGURA_MINIMA_TABELA_PX = 960;

function larguraParaPixels(largura: string | undefined, fallback: number) {
  const match = largura?.trim().match(/^(\d+(?:\.\d+)?)px$/i);
  return match ? Number(match[1]) : fallback;
}

export function calcularLarguraMinimaTabela(
  colunas: ReadonlyArray<{ largura?: string }>,
  larguraColunaPadrao = LARGURA_COLUNA_PADRAO_PX,
  larguraMinima = LARGURA_MINIMA_TABELA_PX,
) {
  const larguraColunas = colunas.reduce(
    (total, coluna) => total + larguraParaPixels(coluna.largura, larguraColunaPadrao),
    0,
  );
  return Math.max(larguraMinima, larguraColunas);
}

export function getColumnSizingStyle(largura?: string): CSSProperties {
  return largura ? { width: largura, minWidth: largura } : {};
}
