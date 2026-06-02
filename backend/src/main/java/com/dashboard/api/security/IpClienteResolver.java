package com.dashboard.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IpClienteResolver {

    private static final String IP_DESCONHECIDO = "unknown";

    private final boolean confiarEmForwardedHeaders;

    public IpClienteResolver(@Value("${security.trust-forwarded-headers:false}") boolean confiarEmForwardedHeaders) {
        this.confiarEmForwardedHeaders = confiarEmForwardedHeaders;
    }

    public String resolver(HttpServletRequest request) {
        if (request == null) {
            return IP_DESCONHECIDO;
        }

        if (confiarEmForwardedHeaders) {
            String ipCloudflare = primeiroValorValido(request.getHeader("CF-Connecting-IP"));
            if (ipCloudflare != null) {
                return ipCloudflare;
            }

            String ipTrueClient = primeiroValorValido(request.getHeader("True-Client-IP"));
            if (ipTrueClient != null) {
                return ipTrueClient;
            }

            String ipForwarded = primeiroValorValido(request.getHeader("X-Forwarded-For"));
            if (ipForwarded != null) {
                return ipForwarded;
            }

            String ipReal = primeiroValorValido(request.getHeader("X-Real-IP"));
            if (ipReal != null) {
                return ipReal;
            }
        }

        String remoteAddr = primeiroValorValido(request.getRemoteAddr());
        return remoteAddr == null ? IP_DESCONHECIDO : remoteAddr;
    }

    private String primeiroValorValido(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .filter(item -> !"unknown".equalsIgnoreCase(item))
                .findFirst()
                .orElse(null);
    }
}
