package com.stockpro.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI authServiceOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("StockPro — Auth Service API")
                        .version("1.0.0")
                        .description("""
                                Authentication and user-management microservice for the StockPro
                                Inventory Management Platform.
                                
                                **Roles:** ADMIN · MANAGER · OFFICER · STAFF
                                
                                **Token flow:**
                                1. `POST /auth/login` → receive `accessToken` + `refreshToken`
                                2. Use `accessToken` in `Authorization: Bearer <token>` header
                                3. `POST /auth/refresh` when access token expires
                                4. `POST /auth/logout` to invalidate session
                                """)
                        .contact(new Contact()
                                .name("StockPro Platform Team")
                                .email("platform@stockpro.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://stockpro.com/license")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort + "/api/v1")
                                .description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token here")));
    }
}
