package com.dashboard.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.dashboard.api.service.acesso.AutenticacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FiltroValidacaoJwt extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroValidacaoJwt.class);

    private final GerenciadorTokenJwt gerenciadorToken;
    private final AutenticacaoService autenticacaoService;

    public FiltroValidacaoJwt(GerenciadorTokenJwt gerenciadorToken, AutenticacaoService autenticacaoService) {
        this.gerenciadorToken = gerenciadorToken;
        this.autenticacaoService = autenticacaoService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (gerenciadorToken.tokenValido(token)) {
                String usuario = gerenciadorToken.extrairUsuario(token);
                try {
                    var authorities = autenticacaoService.authoritiesFor(usuario);

                    if (!authorities.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken autenticacao =
                                new UsernamePasswordAuthenticationToken(usuario, null, authorities);

                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    }
                } catch (Exception ex) {
                    log.error("Falha ao carregar permissões para o usuário '{}': {}", usuario, ex.getMessage(), ex);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
