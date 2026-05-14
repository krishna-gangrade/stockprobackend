package com.stockpro.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * StockPro - API Gateway
 *
 * Single entry point for all client (Angular) requests.
 * Responsibilities:
 *   1. Route /api/v1/** → stockpro-backend (via Eureka load balancer)
 *   2. Validate JWT on every request before forwarding
 *   3. Block unauthenticated requests (except /auth/login, /auth/register)
 *   4. Handle CORS so Angular on port 4200 can reach the backend
 *
 * Runs on port 8082.
 * Angular should point ALL HTTP calls to http://localhost:8082
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
