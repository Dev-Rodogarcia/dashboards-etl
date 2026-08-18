package com.dashboard.api.security;

import com.dashboard.api.support.FakeRateLimitBucketStore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void aplicaLimitesIndependentesParaApiEExportacao() {
        RateLimitService service = new RateLimitService(new FakeRateLimitBucketStore(),
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
        RateLimitService service = new RateLimitService(new FakeRateLimitBucketStore(),
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

    @Test
    void instanciasCompartilhamBucketPersistido() {
        FakeRateLimitBucketStore store = new FakeRateLimitBucketStore();
        RateLimitService primeiraInstancia = new RateLimitService(store, 10, 900, 1, 60, 1, 60);
        RateLimitService segundaInstancia = new RateLimitService(store, 10, 900, 1, 60, 1, 60);

        assertThat(primeiraInstancia.consumirChamadaApi("10.0.0.1:user").permitido()).isTrue();
        assertThat(segundaInstancia.consumirChamadaApi("10.0.0.1:user").permitido()).isFalse();
    }
}
