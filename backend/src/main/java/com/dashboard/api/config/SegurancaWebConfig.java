package com.dashboard.api.config;

import com.dashboard.api.exception.CredencialInvalidaException;
import com.dashboard.api.exception.RespostaErroHttpWriter;
import com.dashboard.api.security.FiltroApiKey;
import com.dashboard.api.security.FiltroRateLimitApi;
import com.dashboard.api.security.FiltroValidacaoJwt;
import com.dashboard.api.service.acesso.AutenticacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SegurancaWebConfig {

    private final FiltroValidacaoJwt filtroJwt;
    private final FiltroApiKey filtroApiKey;
    private final FiltroRateLimitApi filtroRateLimitApi;

    @Value("${security.headers.content-security-policy:default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'}")
    private String contentSecurityPolicy;

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
            AccessDeniedHandler apiAccessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
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
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy()))
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .requestMatcher(request -> true)
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .cacheControl(Customizer.withDefaults())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
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

    private String contentSecurityPolicy() {
        return contentSecurityPolicy == null || contentSecurityPolicy.isBlank()
                ? "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"
                : contentSecurityPolicy.trim();
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
        return (request, response, authException) ->
            RespostaErroHttpWriter.escrever(
                    response,
                    objectMapper,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "Autenticacao obrigatoria ou credenciais invalidas."
            );
    }

    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) ->
                RespostaErroHttpWriter.escrever(
                        response,
                        objectMapper,
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        "Usuario autenticado sem permissao para este recurso."
                );
    }
}
