package com.dashboard.api.config;

import com.dashboard.api.security.FiltroApiKey;
import com.dashboard.api.security.FiltroRateLimitApi;
import com.dashboard.api.security.FiltroValidacaoJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/api/interno/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(filtroApiKey, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(filtroRateLimitApi, FiltroValidacaoJwt.class);

        return http.build();
    }
}
