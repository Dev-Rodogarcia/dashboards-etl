package com.dashboard.api.service;

import com.dashboard.api.client.esl.EslGraphqlClient;
import com.dashboard.api.client.esl.EslGraphqlInputMapper;
import com.dashboard.api.client.esl.EslGraphqlOperations;
import com.dashboard.api.client.esl.EslGraphqlRespostaMapper;
import com.dashboard.api.dto.esl.EslColetaAtualizacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCancelamentoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaListagemRequestDTO;
import com.dashboard.api.dto.esl.EslColetaListagemRespostaDTO;
import com.dashboard.api.dto.esl.EslColetaRespostaDTO;
import com.dashboard.api.dto.esl.EslContextoOperacionalDTO;
import com.dashboard.api.dto.esl.EslCotacaoCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslCotacaoRespostaDTO;
import com.dashboard.api.dto.esl.EslNotaFiscalValidadaDTO;
import com.dashboard.api.exception.EslConflitoEstadoException;
import com.dashboard.api.exception.EslRecursoNaoEncontradoException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

/**
 * Fronteira síncrona do BFF para o ESL. Não possui repositórios nem persiste
 * payloads, identificadores ou recibos operacionais.
 */
@Service
public class EslOperacoesService {

    private final EslGraphqlClient graphqlClient;
    private final EslGraphqlInputMapper inputMapper;
    private final EslGraphqlRespostaMapper respostaMapper;

    public EslOperacoesService(
            EslGraphqlClient graphqlClient,
            EslGraphqlInputMapper inputMapper,
            EslGraphqlRespostaMapper respostaMapper
    ) {
        this.graphqlClient = graphqlClient;
        this.inputMapper = inputMapper;
        this.respostaMapper = respostaMapper;
    }

    public EslCotacaoRespostaDTO criarCotacao(
            EslCotacaoCriacaoRequestDTO solicitacao,
            EslContextoOperacionalDTO contexto
    ) {
        JsonNode recurso = graphqlClient.executarMutation(
                EslGraphqlOperations.QUOTE_CREATE,
                EslGraphqlOperations.MUTATION_QUOTE_CREATE,
                inputMapper.paraQuoteCreate(solicitacao, contexto)
        );
        return respostaMapper.paraCotacao(recurso);
    }

    public EslColetaRespostaDTO criarColeta(
            EslColetaCriacaoRequestDTO solicitacao,
            EslContextoOperacionalDTO contexto
    ) {
        JsonNode recurso = graphqlClient.executarMutation(
                EslGraphqlOperations.PICK_CREATE,
                EslGraphqlOperations.MUTATION_PICK_CREATE,
                inputMapper.paraPickCreate(solicitacao, contexto)
        );
        return respostaMapper.paraColeta(recurso);
    }

    public EslColetaRespostaDTO atualizarColeta(String coletaId, EslColetaAtualizacaoRequestDTO solicitacao) {
        JsonNode recurso = graphqlClient.executarMutation(
                EslGraphqlOperations.PICK_UPDATE,
                EslGraphqlOperations.MUTATION_PICK_UPDATE,
                inputMapper.paraPickUpdate(coletaId, solicitacao)
        );
        return respostaMapper.paraColeta(recurso);
    }

    public EslColetaRespostaDTO cancelarColeta(String coletaId, EslColetaCancelamentoRequestDTO solicitacao) {
        JsonNode recurso = graphqlClient.executarMutation(
                EslGraphqlOperations.PICK_CANCELLATION,
                EslGraphqlOperations.MUTATION_PICK_CANCELLATION,
                inputMapper.paraPickCancellation(coletaId, solicitacao)
        );
        return respostaMapper.paraColeta(recurso);
    }

    public EslColetaListagemRespostaDTO listarColetas(
            EslColetaListagemRequestDTO solicitacao,
            EslContextoOperacionalDTO contexto
    ) {
        JsonNode resultado = graphqlClient.executarQuery(
                EslGraphqlOperations.PICK_LIST,
                EslGraphqlOperations.PICK_LIST_RESULT,
                EslGraphqlOperations.QUERY_PICK_LIST,
                inputMapper.paraPickList(solicitacao, contexto)
        );
        return respostaMapper.paraListagemColetas(resultado);
    }

    public EslNotaFiscalValidadaDTO validarNotaFiscal(String chaveOuNumero) {
        JsonNode resultado = graphqlClient.executarQuery(
                EslGraphqlOperations.INVOICE_LIST,
                EslGraphqlOperations.QUERY_INVOICE_LIST,
                inputMapper.paraInvoiceLookup(chaveOuNumero)
        );
        var notas = respostaMapper.paraNotasFiscais(resultado);
        if (notas.isEmpty()) {
            throw new EslRecursoNaoEncontradoException("Nota fiscal não encontrada no ESL.");
        }
        if (notas.size() > 1) {
            throw new EslConflitoEstadoException(
                    "A busca retornou mais de uma nota fiscal. Informe a chave de acesso para identificar a nota."
            );
        }
        return notas.get(0);
    }
}
