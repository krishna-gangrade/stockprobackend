package com.stockpro.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

/**
 * JwtAuthFilter runs on every routed request BEFORE it reaches stockpro-backend.
 *
 * Flow:
 *   1. Check if the path is public (login, register, swagger) → skip filter
 *   2. Extract Authorization: Bearer <token> header
 *   3. Validate JWT signature and expiry
 *   4. If valid → forward request; add X-User-Email header for backend convenience
 *   5. If invalid → return 401 immediately (backend never sees the request)
 *
 * This means stockpro-backend's own JWT filter is a second layer of defence.
 */
@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilterFactory.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Paths that don't require a JWT token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google",
            "/api/v1/auth/google-login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/refresh-token",
            "/swagger-ui",
            "/swagger-ui.html",
            "/swagger-links",
            "/v3/api-docs",
            "/docs/",
            "/api/v1/swagger-ui",
            "/api/v1/api-docs",
            "/api/v1/v3/api-docs",
            "/actuator"
    );

    public JwtAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip JWT check for public endpoints
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // Check Authorization header exists
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Rejected request without bearer token for path={}", path);
                return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                // Parse and validate JWT (JJWT 0.12.x API)
                Claims claims = Jwts.parser()
                        .verifyWith((javax.crypto.SecretKey) getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Add user info as headers — downstream services can use these
                // instead of re-parsing the JWT
                String userEmail = claims.getSubject();
                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);

                var mutatedRequest = exchange.getRequest()
                        .mutate()
                        .header("X-User-Email", userEmail)
                        .header("X-User-Id", String.valueOf(userId))
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("Rejected expired JWT for path={}", path);
                return unauthorizedResponse(exchange, "Token has expired");
            } catch (JwtException e) {
                log.warn("Rejected invalid JWT for path={} reason={}", path, e.getMessage());
                return unauthorizedResponse(exchange, "Invalid token");
            }
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // Config class required by AbstractGatewayFilterFactory
    public static class Config {
        // No config fields needed for this filter
    }
}
