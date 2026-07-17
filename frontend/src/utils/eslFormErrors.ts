import { isAxiosError } from 'axios';
import { getApiErrorMessage } from './apiError';
import type { EslErroDetalhe, EslErroHttp } from '../types/esl';

export interface EslFormErrors {
  geral: string | null;
  campos: Record<string, string>;
}

type EslErroComDetalhes = EslErroHttp & {
  errors?: Array<EslErroDetalhe | string>;
};

function normalizarTexto(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

function extrairMensagem(erro: EslErroDetalhe | string): string {
  if (typeof erro === 'string') {
    return erro.trim();
  }

  return String(erro.mensagem ?? erro.message ?? '').trim();
}

function extrairCampo(erro: EslErroDetalhe | string): string {
  if (typeof erro === 'string') {
    return '';
  }

  return String(erro.campo ?? erro.field ?? erro.path ?? '').trim();
}

function encontrarCampo(
  campoInformado: string,
  mensagem: string,
  aliasesPorCampo: Record<string, string[]>,
): string | null {
  const alvo = normalizarTexto(`${campoInformado} ${mensagem}`);

  for (const [campo, aliases] of Object.entries(aliasesPorCampo)) {
    if (aliases.some((alias) => alvo.includes(normalizarTexto(alias)))) {
      return campo;
    }
  }

  return null;
}

/**
 * Normaliza o contrato 422 do BFF sem acoplar o formulário ao payload GraphQL.
 * Aceita tanto `erros` (contrato em português) quanto `errors` durante transições.
 */
export function mapearErrosValidacaoEsl(
  error: unknown,
  aliasesPorCampo: Record<string, string[]>,
  fallback = 'O ESL recusou a operação. Revise os dados informados.',
): EslFormErrors {
  if (!isAxiosError(error) || error.response?.status !== 422) {
    return { geral: getApiErrorMessage(error, fallback), campos: {} };
  }

  const payload = error.response.data as EslErroComDetalhes | undefined;
  const detalhes = payload?.erros ?? payload?.errors ?? [];
  const mensagensSemCampo: string[] = [];
  const campos: Record<string, string> = {};

  for (const detalhe of detalhes) {
    const mensagem = extrairMensagem(detalhe);
    if (!mensagem) {
      continue;
    }

    const campo = encontrarCampo(extrairCampo(detalhe), mensagem, aliasesPorCampo);
    if (campo) {
      campos[campo] ??= mensagem;
    } else {
      mensagensSemCampo.push(mensagem);
    }
  }

  const geral = mensagensSemCampo.length > 0
    ? mensagensSemCampo.join(' ')
    : Object.keys(campos).length === 0
      ? getApiErrorMessage(error, fallback)
      : null;

  return { geral, campos };
}
