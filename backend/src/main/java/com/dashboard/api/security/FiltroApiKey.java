package com.dashboard.api.security;

import com.dashboard.api.exception.RespostaErroHttpWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FiltroApiKey extends OncePerRequestFilter {

    private final String apiKeyEsperada;
    private final ObjectMapper objectMapper;

    public FiltroApiKey(
            @Value("${api.interno.key}") String apiKeyEsperada,
            ObjectMapper objectMapper
    ) {
        this.apiKeyEsperada = apiKeyEsperada;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/api/interno")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyRecebida = request.getHeader("X-API-KEY");

        if (apiKeyEsperada.equals(apiKeyRecebida)) {
            // Autentica a requisição interna no SecurityContext
            UsernamePasswordAuthenticationToken autenticacao =
                    new UsernamePasswordAuthenticationToken(
                            "sistema-etl", null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNO"))
                    );
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
            filterChain.doFilter(request, response);
        } else {
            RespostaErroHttpWriter.escrever(
                    response,
                    objectMapper,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "API Key invalida ou ausente."
            );
        }
    }
}
