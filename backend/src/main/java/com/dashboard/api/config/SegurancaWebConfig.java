package com.dashboard.api.config;

import com.dashboard.api.security.FiltroApiKey;
import com.dashboard.api.security.FiltroRateLimitApi;
import com.dashboard.api.security.FiltroValidacaoJwt;
import com.dashboard.api.service.acesso.AutenticacaoService;
import com.dashboard.api.service.acesso.CredencialInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SegurancaWebConfig {

    private final FiltroValidacaoJwt filtroJwt;
    private final FiltroApiKey filtroApiKey;
    private final FiltroRateLimitApi filtroRateLimitApi;

    public SegurancaWebConfig(
            FiltroValidacaoJwt filtroJwt,
            FiltroApiKey filtroApiKey,
            FiltroRateLimitApi filtroRateLimitApi
    ) {
        this.filtroJwt = filtroJwt;
        this.filtroApiKey = filtroApiKey;
        this.filtroRateLimitApi = filtroRateLimitApi;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider basicAuthenticationProvider,
            AuthenticationEntryPoint apiAuthenticationEntryPoint,
            AccessDeniedHandler apiAccessDeniedHandler
    ) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(apiAuthenticationEntryPoint))
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(basicAuthenticationProvider)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers("/api/interno/**").authenticated()
                        .requestMatchers("/api/painel/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(filtroApiKey, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(filtroRateLimitApi, FiltroValidacaoJwt.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider basicAuthenticationProvider(AutenticacaoService autenticacaoService) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String email = authentication.getName() == null ? "" : authentication.getName().trim();
                String senha = authentication.getCredentials() == null
                        ? ""
                        : authentication.getCredentials().toString();

                try {
                    autenticacaoService.autenticar(email, senha);
                    var authorities = autenticacaoService.authoritiesFor(email);
                    return new UsernamePasswordAuthenticationToken(email, null, authorities);
                } catch (CredencialInvalidaException | IllegalArgumentException ex) {
                    throw new BadCredentialsException("Usuario ou senha invalidos.", ex);
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"dashboard-api\"");
            escreverErroJson(
                    response,
                    objectMapper,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Autenticacao obrigatoria ou credenciais invalidas.",
                    request.getRequestURI()
            );
        };
    }

    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) ->
                escreverErroJson(
                        response,
                        objectMapper,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "Usuario autenticado sem permissao para este recurso.",
                        request.getRequestURI()
                );
    }

    private static void escreverErroJson(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String erro,
            String mensagem,
            String path
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("erro", erro);
        body.put("mensagem", mensagem);
        body.put("path", path);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
