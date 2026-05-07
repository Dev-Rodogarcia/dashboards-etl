package com.dashboard.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Objects;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.origens-permitidas:${cors.origem-permitida:http://localhost:5173}}")
    private String origensPermitidas;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(parseOrigensPermitidas())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Content-Disposition", "Content-Length")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @NonNull
    private String[] parseOrigensPermitidas() {
        String[] origens = Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isBlank())
                .toArray(String[]::new);
        return Objects.requireNonNull(origens, "origensPermitidas");
    }
}
