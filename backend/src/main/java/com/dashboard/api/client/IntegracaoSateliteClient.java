package com.dashboard.api.client;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class IntegracaoSateliteClient {

    private static final String ROTA_INTEGRACOES_CLIENTES = "/api/auditoria/integracoes-clientes";
    private static final String ROTA_EVOLUCAO_DIARIA = "/api/auditoria/integracoes-clientes/evolucao-diaria";
    private static final String ROTA_IMAGEM_LOG = "/api/auditoria/logs/{id}/imagem";

    private final RestTemplate restTemplate;
    private final String sateliteBaseUrl;

    public IntegracaoSateliteClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.integration.satelite.url}") String sateliteBaseUrl
    ) {
        this.sateliteBaseUrl = normalizarBaseUrl(sateliteBaseUrl);
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) {
                        return false;
                    }
                })
                .build();
    }

    public ResponseEntity<String> buscarIntegracoesClientes(
            MultiValueMap<String, String> parametros,
            String escopo,
            String dataInicial,
            String dataFinal
    ) {
        MultiValueMap<String, String> parametrosSatelite = new LinkedMultiValueMap<>();
        if (parametros != null) {
            parametrosSatelite.addAll(parametros);
        }
        parametrosSatelite.set("escopo", escopo);
        adicionarParametroOpcional(parametrosSatelite, "dataInicial", dataInicial);
        adicionarParametroOpcional(parametrosSatelite, "dataFinal", dataFinal);

        URI uri = UriComponentsBuilder
                .fromUriString(sateliteBaseUrl + ROTA_INTEGRACOES_CLIENTES)
                .queryParams(parametrosSatelite)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    public ResponseEntity<String> buscarEvolucaoDiaria(String dataInicial, String dataFinal, String escopo) {
        MultiValueMap<String, String> parametrosSatelite = new LinkedMultiValueMap<>();
        adicionarParametroOpcional(parametrosSatelite, "dataInicial", dataInicial);
        adicionarParametroOpcional(parametrosSatelite, "dataFinal", dataFinal);
        adicionarParametroOpcional(parametrosSatelite, "escopo", escopo);

        URI uri = UriComponentsBuilder
                .fromUriString(sateliteBaseUrl + ROTA_EVOLUCAO_DIARIA)
                .queryParams(parametrosSatelite)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    public ResponseEntity<String> buscarImagemLog(Long id) {
        URI uri = UriComponentsBuilder
                .fromUriString(sateliteBaseUrl + ROTA_IMAGEM_LOG)
                .buildAndExpand(id)
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));

        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String normalizarBaseUrl(String valor) {
        String url = valor == null ? "" : valor.trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException("Configure app.integration.satelite.url para habilitar o proxy do Satelite.");
        }
        return url.replaceAll("/+$", "");
    }

    private void adicionarParametroOpcional(MultiValueMap<String, String> parametros, String nome, String valor) {
        if (valor != null && !valor.isBlank()) {
            parametros.set(nome, valor);
        }
    }
}
