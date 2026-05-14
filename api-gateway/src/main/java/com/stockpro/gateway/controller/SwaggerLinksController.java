package com.stockpro.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/docs")
public class SwaggerLinksController {

    @Value("${stockpro.gateway.public-base-url:http://localhost:8082}")
    private String gatewayBaseUrl;

    @GetMapping({"", "/"})
    public Map<String, Object> docsHome() {
        return Map.of(
                "message", "Centralized Swagger/OpenAPI entrypoint for StockPro services",
                "gatewaySwaggerUi", gatewayBaseUrl + "/swagger-ui.html",
                "endpoints", gatewayBaseUrl + "/docs/endpoints",
                "services", serviceLinks()
        );
    }

    @GetMapping("/endpoints")
    public Map<String, Object> swaggerLinks() {
        return Map.of(
                "gatewaySwaggerUi", gatewayBaseUrl + "/swagger-ui.html",
                "services", serviceLinks()
        );
    }

    @GetMapping("/swagger-links")
    public Map<String, Object> legacySwaggerLinks() {
        return swaggerLinks();
    }

    private List<Map<String, String>> serviceLinks() {
        return List.of(
                link("Auth Service", "/docs/auth/swagger-ui.html", "/docs/auth/v3/api-docs"),
                link("Product Service", "/docs/products/swagger-ui.html", "/docs/products/v3/api-docs"),
                link("Warehouse Service", "/docs/warehouses/swagger-ui.html", "/docs/warehouses/v3/api-docs"),
                link("Purchase Service", "/docs/purchases/swagger-ui.html", "/docs/purchases/v3/api-docs"),
                link("Payment Service", "/docs/payments/swagger-ui.html", "/docs/payments/v3/api-docs"),
                link("Supplier Service", "/docs/suppliers/swagger-ui.html", "/docs/suppliers/v3/api-docs"),
                link("Movement Service", "/docs/movements/swagger-ui.html", "/docs/movements/v3/api-docs"),
                link("Alert Service", "/docs/alerts/swagger-ui.html", "/docs/alerts/v3/api-docs"),
                link("Report Service", "/docs/reports/swagger-ui.html", "/docs/reports/v3/api-docs")
        );
    }

    private Map<String, String> link(String name, String swaggerPath, String apiDocsPath) {
        return Map.of(
                "name", name,
                "swaggerUi", gatewayBaseUrl + swaggerPath,
                "apiDocs", gatewayBaseUrl + apiDocsPath
        );
    }
}
