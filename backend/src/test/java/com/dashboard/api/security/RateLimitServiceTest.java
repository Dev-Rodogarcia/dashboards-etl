package com.dashboard.api.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void aplicaLimitesIndependentesParaApiEExportacao() {
        RateLimitService service = new RateLimitService(
                10,
                900,
                2,
                60,
                1,
                60
        );

        assertThat(service.consumirChamadaApi("10.0.0.1:user").permitido()).isTrue();
        assertThat(service.consumirChamadaApi("10.0.0.1:user").permitido()).isTrue();
        assertThat(service.consumirChamadaApi("10.0.0.1:user").permitido()).isFalse();

        assertThat(service.consumirExportacao("10.0.0.1:user").permitido()).isTrue();
        assertThat(service.consumirExportacao("10.0.0.1:user").permitido()).isFalse();
    }

    @Test
    void usuariosDiferentesNaoCompartilhamBucket() {
        RateLimitService service = new RateLimitService(
                10,
                900,
                1,
                60,
                1,
                60
        );

        assertThat(service.consumirChamadaApi("10.0.0.1:user-a").permitido()).isTrue();
        assertThat(service.consumirChamadaApi("10.0.0.1:user-b").permitido()).isTrue();
        assertThat(service.consumirChamadaApi("10.0.0.1:user-a").permitido()).isFalse();
    }
}
