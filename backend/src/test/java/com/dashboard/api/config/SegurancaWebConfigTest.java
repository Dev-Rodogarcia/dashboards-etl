package com.dashboard.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SegurancaWebConfigTest {

    @Test
    void authenticationEntryPointDeveRetornarEnvelopePadraoSemChallengeBasic() throws Exception {
        SegurancaWebConfig config = new SegurancaWebConfig(null, null, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/painel/fretes/exportacao");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        config.apiAuthenticationEntryPoint(objectMapper).commence(request, response, null);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        var json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(json.get("erro").asText()).isEqualTo("Unauthorized");
        assertThat(json.has("path")).isFalse();
    }
}
