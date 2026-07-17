import { describe, expect, it } from 'vitest';
import { isFilialPropria, isParceiroLogistico, separarFiliaisParceiros } from './filiais';

describe('filiais', () => {
  it('mantém as filiais e aliases operacionais da Rodogarcia como próprias', () => {
    expect(isFilialPropria('AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA')).toBe(true);
    expect(isFilialPropria('TR RODOGARCIA | AGU')).toBe(true);
  });

  it('classifica transportadoras e prestadores externos como parceiros', () => {
    expect(isParceiroLogistico('ATF TRANSPORTES | ARARAQUARA')).toBe(true);
    expect(isParceiroLogistico('MASSON EXPRESS')).toBe(true);
    expect(isParceiroLogistico('CR TRANSPORTES E LOGISTICA LTDA')).toBe(true);
    expect(isParceiroLogistico('RODOGAR LOCACAO E SERVICOS')).toBe(true);
  });

  it('separa a dimensão sem promover transportadoras a filiais próprias', () => {
    expect(separarFiliaisParceiros([
      'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA',
      'ATF TRANSPORTES | ARARAQUARA',
      'TR RODOGARCIA | AGU',
    ])).toEqual({
      filiaisProprias: [
        'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA',
        'TR RODOGARCIA | AGU',
      ],
      parceirosLogisticos: ['ATF TRANSPORTES | ARARAQUARA'],
    });
  });
});
