import { describe, expect, it } from 'vitest';
import { criarConfigDownloadExcel, extrairNomeArquivo } from './downloadExcel';

describe('downloadExcel', () => {
  it('cria config axios com responseType blob e filtros f.*', () => {
    const config = criarConfigDownloadExcel({
      dataInicio: '2026-03-17',
      dataFim: '2026-04-16',
      filiais: ['SP', 'RJ'],
    });

    expect(config.responseType).toBe('blob');
    expect(config.params.get('dataInicio')).toBe('2026-03-17');
    expect(config.params.getAll('f.filiais')).toEqual(['SP', 'RJ']);
  });

  it('extrai filename normal e filename UTF-8 do header', () => {
    expect(extrairNomeArquivo('attachment; filename="fretes.xlsx"', 'fallback.xlsx')).toBe('fretes.xlsx');
    expect(extrairNomeArquivo("attachment; filename*=UTF-8''faturas%20abril.xlsx", 'fallback.xlsx')).toBe('faturas abril.xlsx');
  });
});
