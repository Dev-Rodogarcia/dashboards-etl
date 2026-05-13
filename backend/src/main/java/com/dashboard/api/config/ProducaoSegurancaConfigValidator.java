package com.dashboard.api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class ProducaoSegurancaConfigValidator {

    @Value("${spring.profiles.active:}")
    private String springProfilesActive;

    @Value("${app.environment:${ENVIRONMENT:}}")
    private String appEnvironment;

    @Value("${auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Value("${server.address:127.0.0.1}")
    private String serverAddress;

    @Autowired
    private Environment environment;

    @PostConstruct
    void validarConfiguracaoDeProducao() {
        if (!ambienteProducao()) {
            return;
        }

        if (!refreshCookieSecure) {
            throw new IllegalStateException("Produção exige AUTH_REFRESH_COOKIE_SECURE=true.");
        }

        if (!trustForwardedHeaders) {
            throw new IllegalStateException("Produção com Cloudflare Tunnel exige SECURITY_TRUST_FORWARDED_HEADERS=true.");
        }

        String endereco = serverAddress == null ? "" : serverAddress.trim();
        if (!"127.0.0.1".equals(endereco) && !"localhost".equalsIgnoreCase(endereco) && !"::1".equals(endereco)) {
            throw new IllegalStateException("Produção com Cloudflare Tunnel deve manter SERVER_ADDRESS local.");
        }
    }

    private boolean ambienteProducao() {
        String marcadoresAmbiente = marcadoresAmbiente();
        return Arrays.stream(marcadoresAmbiente.split(","))
                .map(String::trim)
                .map(valor -> valor.toLowerCase(Locale.ROOT))
                .anyMatch(valor -> valor.equals("prod") || valor.equals("production") || valor.equals("producao"));
    }

    private String marcadoresAmbiente() {
        if (environment != null && environment.getActiveProfiles().length > 0) {
            return String.join(",", environment.getActiveProfiles());
        }
        return springProfilesActive + "," + appEnvironment;
    }
}
