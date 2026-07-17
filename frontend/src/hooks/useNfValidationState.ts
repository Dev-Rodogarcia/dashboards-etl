import { useCallback, useEffect, useMemo, useState } from 'react';
import type { EslNotaFiscalValidada } from '../types/esl';

export interface NfValidationDependencies {
  cnpj: string;
  remetente: string;
  filial: string;
  filialOperacional: string;
  chaveOrNumeroNf: string;
}

export type NfValidationStatus = 'nao_validado' | 'validado';

interface NfValidationRecord {
  dependencias: NfValidationDependencies;
  notaFiscal: EslNotaFiscalValidada;
}

export interface NfValidationSnapshot {
  status: NfValidationStatus;
  invoiceId: string | null;
  notaFiscal: EslNotaFiscalValidada | null;
  podeSubmeter: boolean;
}

export interface UseNfValidationStateResult extends NfValidationSnapshot {
  registrarNotaFiscalValidada: (
    notaFiscal: EslNotaFiscalValidada,
    dependenciasValidadas?: NfValidationDependencies,
  ) => void;
  invalidarNotaFiscal: () => void;
}

export function dependenciasNfSaoIguais(
  primeira: NfValidationDependencies,
  segunda: NfValidationDependencies,
): boolean {
  return primeira.cnpj === segunda.cnpj
    && primeira.remetente === segunda.remetente
    && primeira.filial === segunda.filial
    && primeira.filialOperacional === segunda.filialOperacional
    && primeira.chaveOrNumeroNf === segunda.chaveOrNumeroNf;
}

export function resolverNfValidationSnapshot(
  registro: NfValidationRecord | null,
  dependenciasAtuais: NfValidationDependencies,
): NfValidationSnapshot {
  if (!registro || !dependenciasNfSaoIguais(registro.dependencias, dependenciasAtuais)) {
    return {
      status: 'nao_validado',
      invoiceId: null,
      notaFiscal: null,
      podeSubmeter: false,
    };
  }

  const invoiceId = registro.notaFiscal.invoiceId || null;
  return {
    status: invoiceId ? 'validado' : 'nao_validado',
    invoiceId,
    notaFiscal: invoiceId ? registro.notaFiscal : null,
    podeSubmeter: Boolean(invoiceId),
  };
}

/**
 * Mantém somente em memória o invoiceId retornado pelo ESL. O identificador é
 * exposto apenas enquanto todos os dados usados na validação permanecem idênticos.
 */
export function useNfValidationState(
  dependencias: NfValidationDependencies,
): UseNfValidationStateResult {
  const [registro, setRegistro] = useState<NfValidationRecord | null>(null);

  const dependenciasAtuais = useMemo<NfValidationDependencies>(() => ({
    cnpj: dependencias.cnpj,
    remetente: dependencias.remetente,
    filial: dependencias.filial,
    filialOperacional: dependencias.filialOperacional,
    chaveOrNumeroNf: dependencias.chaveOrNumeroNf,
  }), [
    dependencias.chaveOrNumeroNf,
    dependencias.cnpj,
    dependencias.filial,
    dependencias.filialOperacional,
    dependencias.remetente,
  ]);

  const snapshot = resolverNfValidationSnapshot(registro, dependenciasAtuais);

  useEffect(() => {
    if (registro && !dependenciasNfSaoIguais(registro.dependencias, dependenciasAtuais)) {
      setRegistro(null);
    }
  }, [dependenciasAtuais, registro]);

  const registrarNotaFiscalValidada = useCallback((
    notaFiscal: EslNotaFiscalValidada,
    dependenciasValidadas: NfValidationDependencies = dependenciasAtuais,
  ) => {
    setRegistro({
      notaFiscal,
      dependencias: { ...dependenciasValidadas },
    });
  }, [dependenciasAtuais]);

  const invalidarNotaFiscal = useCallback(() => {
    setRegistro(null);
  }, []);

  return {
    ...snapshot,
    registrarNotaFiscalValidada,
    invalidarNotaFiscal,
  };
}
