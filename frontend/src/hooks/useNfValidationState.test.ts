// @vitest-environment jsdom

import { act, cleanup, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { useNfValidationState } from './useNfValidationState';
import type { EslNotaFiscalValidada } from '../types/esl';
import type { NfValidationDependencies } from './useNfValidationState';

const dependenciasIniciais: NfValidationDependencies = {
  cnpj: '12345678000199',
  remetente: '98765432000155',
  filial: 'MATRIZ',
  filialOperacional: 'SPO',
  chaveOrNumeroNf: '35260112345678000199550010000000011000000010',
};

const notaFiscalValidada: EslNotaFiscalValidada = {
  invoiceId: 'esl-invoice-123',
  chaveAcesso: dependenciasIniciais.chaveOrNumeroNf,
  numero: '1',
  serie: '1',
  dataEmissao: '2026-07-16',
  status: 'APPROVED',
  valor: 1200,
  peso: 35,
  volume: 2,
};

afterEach(() => {
  cleanup();
});

describe('useNfValidationState', () => {
  it.each<keyof NfValidationDependencies>([
    'cnpj',
    'remetente',
    'filial',
    'filialOperacional',
    'chaveOrNumeroNf',
  ])('invalida o invoiceId quando %s é alterado', (campoAlterado) => {
    const { result, rerender } = renderHook(
      ({ dependencias }: { dependencias: NfValidationDependencies }) => useNfValidationState(dependencias),
      { initialProps: { dependencias: dependenciasIniciais } },
    );

    act(() => {
      result.current.registrarNotaFiscalValidada(notaFiscalValidada, dependenciasIniciais);
    });

    expect(result.current).toMatchObject({
      status: 'validado',
      invoiceId: 'esl-invoice-123',
      podeSubmeter: true,
    });

    const dependenciasAlteradas = {
      ...dependenciasIniciais,
      [campoAlterado]: `${dependenciasIniciais[campoAlterado]}0`,
    };
    rerender({ dependencias: dependenciasAlteradas });

    expect(result.current).toMatchObject({
      status: 'nao_validado',
      invoiceId: null,
      notaFiscal: null,
      podeSubmeter: false,
    });

    rerender({ dependencias: dependenciasIniciais });
    expect(result.current).toMatchObject({
      status: 'nao_validado',
      invoiceId: null,
      podeSubmeter: false,
    });
  });
});
