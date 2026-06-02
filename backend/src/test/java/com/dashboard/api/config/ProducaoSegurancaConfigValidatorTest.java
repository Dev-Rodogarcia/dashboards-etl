package com.dashboard.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProducaoSegurancaConfigValidatorTest {

    @Test
    void devePermitirConfiguracaoSeguraDeProducaoComTunnel() {
        ProducaoSegurancaConfigValidator validator = validator(
                "prod",
                "",
                true,
                true,
                "127.0.0.1"
        );

        assertThatCode(validator::validarConfiguracaoDeProducao).doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarCookieInseguroEmProducao() {
        ProducaoSegurancaConfigValidator validator = validator(
                "prod",
                "",
                false,
                true,
                "127.0.0.1"
        );

        assertThatThrownBy(validator::validarConfiguracaoDeProducao)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_REFRESH_COOKIE_SECURE=true");
    }

    @Test
    void deveRejeitarBindPublicoEmProducao() {
        ProducaoSegurancaConfigValidator validator = validator(
                "prod",
                "",
                true,
                true,
                "0.0.0.0"
        );

        assertThatThrownBy(validator::validarConfiguracaoDeProducao)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SERVER_ADDRESS local");
    }

    @Test
    void deveRejeitarForwardedHeadersDesabilitadoEmProducao() {
        ProducaoSegurancaConfigValidator validator = validator(
                "prod",
                "",
                true,
                false,
                "127.0.0.1"
        );

        assertThatThrownBy(validator::validarConfiguracaoDeProducao)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECURITY_TRUST_FORWARDED_HEADERS=true");
    }

    @Test
    void devePermitirConfiguracaoLocalForaDeProducao() {
        ProducaoSegurancaConfigValidator validator = validator(
                "dev",
                "",
                false,
                false,
                "0.0.0.0"
        );

        assertThatCode(validator::validarConfiguracaoDeProducao).doesNotThrowAnyException();
    }

    private static ProducaoSegurancaConfigValidator validator(
            String profiles,
            String environment,
            boolean refreshCookieSecure,
            boolean trustForwardedHeaders,
            String serverAddress
    ) {
        ProducaoSegurancaConfigValidator validator = new ProducaoSegurancaConfigValidator();
        ReflectionTestUtils.setField(validator, "springProfilesActive", profiles);
        ReflectionTestUtils.setField(validator, "appEnvironment", environment);
        ReflectionTestUtils.setField(validator, "refreshCookieSecure", refreshCookieSecure);
        ReflectionTestUtils.setField(validator, "trustForwardedHeaders", trustForwardedHeaders);
        ReflectionTestUtils.setField(validator, "serverAddress", serverAddress);
        return validator;
    }
}
