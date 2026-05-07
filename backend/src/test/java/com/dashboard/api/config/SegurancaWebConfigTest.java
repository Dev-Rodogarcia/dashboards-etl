package com.dashboard.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SegurancaWebConfigTest {

    @Test
    void authenticationEntryPointDeveEnviarWwwAuthenticateParaAbrirPromptBasicDoBrowser() throws Exception {
        SegurancaWebConfig config = new SegurancaWebConfig(null, null, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/painel/fretes/exportacao");
        MockHttpServletResponse response = new MockHttpServletResponse();

        config.apiAuthenticationEntryPoint(new ObjectMapper()).commence(request, response, null);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"dashboard-api\"");
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"status\":401");
    }
}
