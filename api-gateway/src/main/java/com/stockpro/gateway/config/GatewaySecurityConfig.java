package com.stockpro.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway security configuration.
 *
 * The gateway uses WebFlux (reactive), so we use ServerHttpSecurity
 * instead of the regular HttpSecurity used in stockpro-backend.
 *
 * We disable Spring Security's default auth here because our
 * custom JwtAuthFilter (a GatewayFilter) handles authentication.
 * Spring Security at this layer would add an unnecessary second login prompt.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchange -> exchange
                    .anyExchange().permitAll()   // JWT filter handles auth, not Spring Security
            );

        return http.build();
    }
}
