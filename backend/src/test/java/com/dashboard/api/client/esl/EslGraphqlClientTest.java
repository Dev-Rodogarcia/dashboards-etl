package com.dashboard.api.client.esl;

import com.dashboard.api.exception.EslGraphqlOperacaoRecusadaException;
import com.dashboard.api.exception.EslGraphqlTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EslGraphqlClientTest {

    @Test
    void deveAdiarValidacaoDaConfiguracaoEslAteAPrimeiraOperacao() {
        EslGraphqlClient client = new EslGraphqlClient(
                new EslGraphqlProperties(
                        "https://esl.example/graphql", "", null, null, null, null, null, null, null
                ),
                new EslGraphqlPayloadNormalizer()
        );

        assertThatThrownBy(() -> client.executarQuery("pick", "query pick { }", Map.of()))
                .isInstanceOf(com.dashboard.api.exception.EslGraphqlConfiguracaoException.class)
                .hasMessageContaining("ESL_GRAPHQL_TOKEN");
    }

    @Test
    void devePriorizarBaseEEndpointCanonicosDoEtlSobreConfiguracaoLegada() {
        EslGraphqlProperties properties = new EslGraphqlProperties(
                "token-legado-que-nao-e-url", "token", null, null, null, null, null,
                "https://esl.example", "/graphql"
        );

        assertThat(EslGraphqlClient.resolverUrlGraphql(properties))
                .isEqualTo("https://esl.example/graphql");
    }

    @Test
    void deveAceitarCampoDeResultadoDiferenteDoOperationNameNaConsultaGraphql() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("""
                                        {"data":{"pick":{"edges":[],"pageInfo":{"hasNextPage":false}}}}
                                        """)
                                .build()
                ))
                .build();
        EslGraphqlClient client = new EslGraphqlClient(
                webClient,
                new EslGraphqlPayloadNormalizer(),
                EslGraphqlClient.TIMEOUT_RIGIDO
        );

        JsonNode resultado = client.executarQuery("pickList", "pick", "query pickList { pick { edges { cursor } } }", Map.of());

        assertThat(resultado.path("edges")).isEmpty();
    }

    @Test
    void deveConverterErrosGraphqlEmExcecaoDeDominioMesmoComHttp200() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("""
                                        {
                                          "data": {
                                            "quoteCreate": {
                                              "success": false,
                                              "errors": ["CEP de destino inválido"],
                                              "resource": null
                                            }
                                          }
                                        }
                                        """)
                                .build()
                ))
                .build();
        EslGraphqlClient client = new EslGraphqlClient(
                webClient,
                new EslGraphqlPayloadNormalizer(),
                EslGraphqlClient.TIMEOUT_RIGIDO
        );

        assertThatThrownBy(() -> client.executarMutation("quoteCreate", "mutation quoteCreate { }", Map.of()))
                .isInstanceOf(EslGraphqlOperacaoRecusadaException.class)
                .hasMessageContaining("CEP de destino inválido");
    }

    @Test
    void deveCancelarRequisicaoLentaAoAtingirTimeoutSemAguardarRespostaExterna() {
        AtomicBoolean requisicaoCancelada = new AtomicBoolean(false);
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.<ClientResponse>never()
                        .doOnCancel(() -> requisicaoCancelada.set(true)))
                .build();
        EslGraphqlClient client = new EslGraphqlClient(
                webClient,
                new EslGraphqlPayloadNormalizer(),
                Duration.ofMillis(100)
        );
        long inicio = System.nanoTime();

        assertThatThrownBy(() -> client.executarQuery("pick", "query pick { }", Map.of()))
                .isInstanceOf(EslGraphqlTimeoutException.class);

        Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);
        assertThat(requisicaoCancelada).isTrue();
        assertThat(duracao).isLessThan(Duration.ofSeconds(1));
        assertThat(EslGraphqlClient.TIMEOUT_RIGIDO).isEqualTo(Duration.ofSeconds(6));
    }
}
