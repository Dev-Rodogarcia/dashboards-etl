package com.dashboard.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void deveExporContentDispositionParaDownloadCsvNoFrontend() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(
                config,
                "origensPermitidas",
                "http://localhost:5173,https://analytics.rodogarcia.com.br"
        );
        TestCorsRegistry registry = new TestCorsRegistry();

        config.addCorsMappings(registry);

        CorsConfiguration cors = registry.configuracoes().get("/api/**");
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains("https://analytics.rodogarcia.com.br");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type");
        assertThat(cors.getExposedHeaders()).contains("Content-Disposition", "Content-Length");
    }

    private static class TestCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configuracoes() {
            return getCorsConfigurations();
        }
    }
}
