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
                link("Auth Service", "auth"),
                link("Product Service", "products"),
                link("Warehouse Service", "warehouses"),
                link("Purchase Service", "purchases"),
                link("Payment Service", "payments"),
                link("Supplier Service", "suppliers"),
                link("Movement Service", "movements"),
                link("Alert Service", "alerts"),
                link("Report Service", "reports")
        );
    }

    private Map<String, String> link(String name, String slug) {
        String apiDocsUrl = gatewayBaseUrl + "/docs/" + slug + "/v3/api-docs";
        return Map.of(
                "name", name,
                "swaggerUi", gatewayBaseUrl + "/swagger-ui.html?url=" + encode(apiDocsUrl),
                "apiDocs", apiDocsUrl
        );
    }

    private String encode(String value) {
        return value.replace(" ", "%20");
    }
}
