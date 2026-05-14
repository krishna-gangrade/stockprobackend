package com.stockpro.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * StockPro - Spring Boot Admin Server
 *
 * Automatically discovers all services registered in Eureka
 * and displays their health, metrics, logs, and environment
 * in a single web dashboard.
 *
 * Dashboard available at: http://localhost:9090
 * Login: admin / admin-secret  (set in application.properties)
 *
 * Services it monitors:
 *   - eureka-server    (port 8761)
 *   - api-gateway      (port 8082)
 *   - stockpro-backend (port 8081)
 */
@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
public class AdminServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
