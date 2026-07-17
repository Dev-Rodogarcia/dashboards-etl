package com.dashboard.api.client.esl;

import com.dashboard.api.exception.EslGraphqlConfiguracaoException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EslCorporacaoResolverTest {

    @Test
    void deveResolverCodigoOuNomeCompletoDaFilialEReutilizarCache() {
        AtomicInteger chamadas = new AtomicInteger();
        EslCorporacaoResolver resolver = new EslCorporacaoResolver(clientComCorporacoes(chamadas));

        var corporacao = resolver.resolverCorporacao("SPO");
        assertThat(corporacao.documento()).isEqualTo("60960000024300");
        assertThat(corporacao.id()).isEqualTo(101L);
        assertThat(resolver.resolverDocumentoCorporacao("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"))
                .isEqualTo("60960000024300");
        assertThat(chamadas).hasValue(1);
    }

    @Test
    void deveRecusarFilialSemCorporacaoEslAtiva() {
        EslCorporacaoResolver resolver = new EslCorporacaoResolver(clientComCorporacoes(new AtomicInteger()));

        assertThatThrownBy(() -> resolver.resolverDocumentoCorporacao("FILIAL INEXISTENTE"))
                .isInstanceOf(EslGraphqlConfiguracaoException.class)
                .hasMessageContaining("não possui uma corporação ESL ativa");
    }

    private EslGraphqlClient clientComCorporacoes(AtomicInteger chamadas) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    chamadas.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body("""
                                    {"data":{"company":{"edges":[
                                      {"cursor":"c1","node":{"cnpj":"60.960.000/0243-00","name":"SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA","nickname":"SPO - RODOGARCIA","corporation":{"id":"101","person":{"cnpj":"60.960.000/0243-00"}}}},
                                      {"cursor":"c2","node":{"cnpj":"60.960.000/0677-00","name":"CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA","nickname":"CWB - RODOGARCIA","corporation":{"id":102,"person":{"cnpj":"60.960.000/0677-00"}}}}
                                    ],"pageInfo":{"hasNextPage":false,"endCursor":null}}}}
                                    """)
                            .build());
                })
                .build();
        return new EslGraphqlClient(webClient, new EslGraphqlPayloadNormalizer(), EslGraphqlClient.TIMEOUT_RIGIDO);
    }
}
