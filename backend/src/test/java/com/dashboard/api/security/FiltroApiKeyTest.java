package com.dashboard.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroApiKeyTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarEnvelopePadraoQuandoApiKeyAusente() throws Exception {
        FiltroApiKey filtro = new FiltroApiKey("segredo", objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/interno/processamento");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_NO_CONTENT);

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        var json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(json.get("erro").asText()).isEqualTo("Unauthorized");
        assertThat(json.get("mensagem").asText()).isEqualTo("API Key invalida ou ausente.");
        assertThat(json.has("path")).isFalse();
    }

    @Test
    void deveAutenticarRequisicaoInternaComApiKeyCorreta() throws Exception {
        FiltroApiKey filtro = new FiltroApiKey("segredo", objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/interno/processamento");
        request.addHeader("X-API-KEY", "segredo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_NO_CONTENT);

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("sistema-etl");
    }
}
