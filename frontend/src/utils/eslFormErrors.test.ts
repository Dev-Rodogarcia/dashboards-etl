import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { mapearErrosValidacaoEsl } from './eslFormErrors';

describe('mapearErrosValidacaoEsl', () => {
  it('vincula detalhes 422 aos campos do formulário sem expor payload GraphQL', () => {
    const erro = new AxiosError(
      'Unprocessable Entity',
      undefined,
      undefined,
      undefined,
      {
        status: 422,
        data: {
          mensagem: 'Dados recusados pelo ESL.',
          erros: [
            { campo: 'trechos[0].cepDestino', mensagem: 'CEP de destino inválido.' },
            { field: 'documentoCliente', message: 'CNPJ inválido.' },
          ],
        },
      } as never,
    );

    const resultado = mapearErrosValidacaoEsl(erro, {
      cepDestino: ['cepDestino', 'CEP de destino'],
      documentoCliente: ['documentoCliente', 'CNPJ'],
    });

    expect(resultado).toEqual({
      geral: null,
      campos: {
        cepDestino: 'CEP de destino inválido.',
        documentoCliente: 'CNPJ inválido.',
      },
    });
  });
});
