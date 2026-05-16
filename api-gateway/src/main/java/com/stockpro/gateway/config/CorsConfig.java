package com.stockpro.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gateway-level CORS config.
 *
 * Since ALL requests go through the gateway, CORS only needs to be
 * configured here — not in stockpro-backend. The Angular app on
 * http://localhost:4200 is the only permitted origin in development.
 *
 * In production: replace localhost:4200 with your actual frontend domain.
 */
@Configuration
public class CorsConfig {
    @Value("${stockpro.gateway.allowed-origins:http://localhost:4200,http://localhost:80,https://stockpro.linkpc.net,http://stockpro.linkpc.net}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Auth-Error"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
