package com.dashboard.api.client.esl;

import com.dashboard.api.exception.EslGraphqlComunicacaoException;
import com.dashboard.api.exception.EslGraphqlConfiguracaoException;
import com.dashboard.api.exception.EslGraphqlTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Component
public class EslGraphqlClient {

    public static final Duration TIMEOUT_RIGIDO = Duration.ofSeconds(6);

    private volatile WebClient webClient;
    private final EslGraphqlProperties properties;
    private final EslGraphqlPayloadNormalizer normalizer;
    private final Duration timeout;

    @Autowired
    public EslGraphqlClient(
            EslGraphqlProperties properties,
            EslGraphqlPayloadNormalizer normalizer
    ) {
        this(null, properties, normalizer, TIMEOUT_RIGIDO);
    }

    EslGraphqlClient(WebClient webClient, EslGraphqlPayloadNormalizer normalizer, Duration timeout) {
        this(webClient, null, normalizer, timeout);
    }

    private EslGraphqlClient(
            WebClient webClient,
            EslGraphqlProperties properties,
            EslGraphqlPayloadNormalizer normalizer,
            Duration timeout
    ) {
        this.webClient = webClient;
        this.properties = properties;
        this.normalizer = normalizer;
        this.timeout = timeout;
    }

    public JsonNode executarMutation(String operacao, String query, Map<String, Object> variaveis) {
        JsonNode resposta = executar(operacao, query, variaveis);
        return normalizer.extrairRecursoMutation(resposta, operacao);
    }

    public JsonNode executarQuery(String operacao, String query, Map<String, Object> variaveis) {
        return executarQuery(operacao, operacao, query, variaveis);
    }

    public JsonNode executarQuery(
            String operationName,
            String campoResultado,
            String query,
            Map<String, Object> variaveis
    ) {
        JsonNode resposta = executar(operationName, query, variaveis);
        return normalizer.extrairResultadoQuery(resposta, campoResultado);
    }

    private JsonNode executar(String operacao, String query, Map<String, Object> variaveis) {
        try {
            return obterWebClient().post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(new EslGraphqlRequest(operacao, query, variaveis))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .switchIfEmpty(Mono.error(new EslGraphqlComunicacaoException(
                            new IllegalStateException("Resposta HTTP do ESL sem corpo")
                    )))
                    .timeout(timeout)
                    .block(timeout.plusMillis(100));
        } catch (RuntimeException ex) {
            throw traduzirFalha(ex);
        }
    }

    private RuntimeException traduzirFalha(RuntimeException ex) {
        Throwable causa = Exceptions.unwrap(ex);
        if (causa instanceof EslGraphqlComunicacaoException
                || causa instanceof EslGraphqlConfiguracaoException
                || causa instanceof EslGraphqlTimeoutException
                || causa instanceof com.dashboard.api.exception.EslGraphqlOperacaoRecusadaException) {
            return (RuntimeException) causa;
        }
        if (possuiCausaTimeout(causa)) {
            return new EslGraphqlTimeoutException(causa);
        }
        return new EslGraphqlComunicacaoException(causa);
    }

    private boolean possuiCausaTimeout(Throwable throwable) {
        Throwable atual = throwable;
        while (atual != null) {
            if (atual instanceof TimeoutException
                    || atual instanceof SocketTimeoutException
                    || atual instanceof ReadTimeoutException) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }

    private WebClient obterWebClient() {
        WebClient client = webClient;
        if (client != null) {
            return client;
        }

        synchronized (this) {
            if (webClient == null) {
                webClient = criarWebClient(properties);
            }
            return webClient;
        }
    }

    private static WebClient criarWebClient(EslGraphqlProperties properties) {
        String graphqlUrl = resolverUrlGraphql(properties);
        String token = properties.bearerToken();
        if (!StringUtils.hasText(token)) {
            throw new EslGraphqlConfiguracaoException(
                    "Configure ESL_GRAPHQL_TOKEN ou API_GRAPHQL_TOKEN para habilitar a integração ESL."
            );
        }

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(TIMEOUT_RIGIDO.toMillis()))
                .responseTimeout(TIMEOUT_RIGIDO);

        return WebClient.builder()
                .baseUrl(removerBarraFinal(graphqlUrl))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                .build();
    }

    static String resolverUrlGraphql(EslGraphqlProperties properties) {
        String baseUrl = properties.apiBaseUrl();
        String graphqlUrl = StringUtils.hasText(baseUrl)
                ? combinarBaseComEndpoint(baseUrl, properties.graphqlEndpoint())
                : properties.graphqlUrl();
        if (!StringUtils.hasText(graphqlUrl)) {
            throw new EslGraphqlConfiguracaoException(
                    "Configure ESL_GRAPHQL_URL ou API_BASEURL para habilitar a integração ESL."
            );
        }

        String normalizada = removerBarraFinal(graphqlUrl);
        try {
            URI uri = new URI(normalizada);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())
                    || !StringUtils.hasText(uri.getHost())) {
                throw new EslGraphqlConfiguracaoException(
                        "A URL da integração ESL deve ser HTTP(S). Configure ESL_GRAPHQL_URL ou API_BASEURL."
                );
            }
            return normalizada;
        } catch (URISyntaxException ex) {
            throw new EslGraphqlConfiguracaoException(
                    "A URL da integração ESL é inválida. Configure ESL_GRAPHQL_URL ou API_BASEURL."
            );
        }
    }

    private static String combinarBaseComEndpoint(String baseUrl, String endpoint) {
        String caminho = StringUtils.hasText(endpoint) ? endpoint.trim() : "/graphql";
        return removerBarraFinal(baseUrl) + "/" + caminho.replaceFirst("^/+", "");
    }

    private static String removerBarraFinal(String url) {
        return url.trim().replaceAll("/+$", "");
    }
}
