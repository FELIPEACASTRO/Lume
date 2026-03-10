package com.lume.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuracao de CORS.
 *
 * <p>O backend nao presume nenhum cliente cross-origin por padrao.
 * Quando necessario, as origens permitidas devem ser informadas por
 * `CORS_ALLOWED_ORIGINS`.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = parseAllowedOrigins();
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    private List<String> parseAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }
}
