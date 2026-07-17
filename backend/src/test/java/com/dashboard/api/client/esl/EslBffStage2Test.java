package com.dashboard.api.client.esl;

import com.dashboard.api.config.EslContextoOperacionalProvider;
import com.dashboard.api.controller.EslColetasController;
import com.dashboard.api.controller.EslCotacoesController;
import com.dashboard.api.dto.esl.EslColetaAtualizacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCancelamentoRequestDTO;
import com.dashboard.api.dto.esl.EslCotacaoCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslMotivoCancelamento;
import com.dashboard.api.exception.EslConflitoEstadoException;
import com.dashboard.api.exception.EslRecursoNaoEncontradoException;
import com.dashboard.api.service.EslOperacoesService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EslBffStage2Test {

    @Test
    void deveRetornarNotaFiscalValidadaQuandoEslEncontrarUmUnicoResultado() {
        EslOperacoesService service = serviceComResposta("""
                {"data":{"invoice":{"edges":[{"node":{
                  "id":"8950942","key":"351...","number":"42","series":"1",
                  "issuedAt":"2026-01-10","status":"active","value":1500.00,"weight":10.000,"volume":0.220
                }}]}}}
                """);

        var resposta = service.validarNotaFiscal("42");

        assertThat(resposta.invoiceId()).isEqualTo("8950942");
        assertThat(resposta.numero()).isEqualTo("42");
    }

    @Test
    void deveConverterNotaFiscalAusenteENumeroAmbiguoEmErrosDeDominio() {
        EslOperacoesService ausente = serviceComResposta("{\"data\":{\"invoice\":{\"edges\":[]}}}");
        EslOperacoesService ambiguo = serviceComResposta("""
                {"data":{"invoice":{"edges":[
                  {"node":{"id":"1","number":"42"}},
                  {"node":{"id":"2","number":"42"}}
                ]}}}
                """);

        assertThatThrownBy(() -> ausente.validarNotaFiscal("999"))
                .isInstanceOf(EslRecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> ambiguo.validarNotaFiscal("42"))
                .isInstanceOf(EslConflitoEstadoException.class)
                .hasMessageContaining("mais de uma nota fiscal");
    }

    @Test
    void deveExporRecibosComStatusCorretosEUsarIdDaRotaNasOperacoesDeColeta() {
        EslOperacoesService service = serviceComResposta("""
                {"data":{
                  "quoteCreate":{"success":true,"errors":[],"resource":{
                    "id":"126507","sequenceCode":2104,"referenceNumber":"Ref","effectiveUntil":"2026-07-17",
                    "bidsPendingCount":1,"quoteStretchBids":[{"total":3753.00}]
                  }},
                  "pickUpdate":{"success":true,"errors":[],"resource":{"id":"69984","sequenceCode":53,"status":"pending"}},
                  "pickCancellation":{"success":true,"errors":[],"resource":{"id":"69984","sequenceCode":53,"status":"canceled","cancellationReason":"Duplicidade"}},
                  "invoice":{"edges":[{"node":{"id":"8950942","number":"42"}}]}
                }}
                """);
        EslContextoOperacionalProvider contextoProvider = contextoProvider();
        EslCotacoesController cotacoes = new EslCotacoesController(service, contextoProvider);
        EslColetasController coletas = new EslColetasController(service, contextoProvider);

        var cotacao = cotacoes.criar("SPO", new EslCotacaoCriacaoRequestDTO(
                "123", LocalDate.of(2026, 7, 17), "Ref", null, List.of()
        ));
        var nota = coletas.validarNotaFiscal("42", "SPO");
        var atualizacao = coletas.atualizar("69984", "SPO", new EslColetaAtualizacaoRequestDTO(
                null, null, LocalDate.of(2026, 7, 20), null, null, null, null, "Novo horário"
        ));
        var cancelamento = coletas.cancelar("69984", "SPO", new EslColetaCancelamentoRequestDTO(
                EslMotivoCancelamento.DUPLICIDADE
        ));

        assertThat(cotacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cotacao.getBody().valorFreteTotal()).isEqualByComparingTo("3753.00");
        assertThat(nota.getBody().invoiceId()).isEqualTo("8950942");
        assertThat(atualizacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atualizacao.getBody().coletaId()).isEqualTo("69984");
        assertThat(cancelamento.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelamento.getBody().status()).isEqualTo("canceled");
    }

    private EslOperacoesService serviceComResposta(String resposta) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(resposta)
                                .build()
                ))
                .build();
        EslGraphqlClient client = new EslGraphqlClient(
                webClient,
                new EslGraphqlPayloadNormalizer(),
                EslGraphqlClient.TIMEOUT_RIGIDO
        );
        return new EslOperacoesService(
                client,
                new EslGraphqlInputMapper(),
                new EslGraphqlRespostaMapper()
        );
    }

    private EslContextoOperacionalProvider contextoProvider() {
        return new EslContextoOperacionalProvider(new EslGraphqlProperties(
                "https://esl.example/graphql",
                "token",
                "12345678000190",
                "Integração Dashboard",
                "integracao@empresa.com.br",
                null,
                "Operações",
                null,
                null
        ));
    }
}
