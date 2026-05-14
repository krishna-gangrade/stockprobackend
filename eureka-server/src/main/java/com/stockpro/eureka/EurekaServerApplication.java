package com.stockpro.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * StockPro - Eureka Service Discovery Server
 *
 * Every microservice in StockPro registers itself here on startup.
 * The API Gateway uses Eureka to resolve service names to real URLs
 * (e.g. "stockpro-backend" → http://localhost:8082).
 *
 * Dashboard available at: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
