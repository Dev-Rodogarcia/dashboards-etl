import { describe, expect, it } from 'vitest';
import {
  filtrarPorDestinosSelecionados,
  respostaContemDestinoForaDaSelecao,
} from './integracoesDestinoFilter';

const metricas = [
  { sistemaDestino: 'PPG', totalRegistros: 0 },
  { sistemaDestino: 'VEDACIT', totalRegistros: 32 },
  { sistemaDestino: 'SELIA', totalRegistros: 0 },
];

describe('integracoesDestinoFilter', () => {
  it('mantém todos os destinos quando não há seleção', () => {
    expect(filtrarPorDestinosSelecionados(metricas, [])).toEqual(metricas);
    expect(respostaContemDestinoForaDaSelecao(metricas, [])).toBe(false);
  });

  it('mantém apenas o destino selecionado', () => {
    expect(filtrarPorDestinosSelecionados(metricas, ['PPG'])).toEqual([
      { sistemaDestino: 'PPG', totalRegistros: 0 },
    ]);
  });

  it('detecta resposta do Satélite que ignora a seleção', () => {
    expect(respostaContemDestinoForaDaSelecao(metricas, ['SELIA'])).toBe(true);
    expect(respostaContemDestinoForaDaSelecao([
      { sistemaDestino: 'SELIA', totalRegistros: 0 },
    ], ['SELIA'])).toBe(false);
  });

  it('desconsidera destinos externos zerados na resposta de métricas', () => {
    expect(respostaContemDestinoForaDaSelecao(
      [
        { sistemaDestino: 'PPG', totalRegistros: 0 },
        { sistemaDestino: 'VEDACIT', totalRegistros: 32 },
      ],
      ['VEDACIT'],
      (item) => item.totalRegistros > 0,
    )).toBe(false);
  });
});
