package com.stockpro.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/{slug}/swagger-config")
    public Map<String, Object> swaggerConfig(@PathVariable String slug) {
        if (serviceLinks().stream().noneMatch(link -> slug.equals(link.get("slug")))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown docs service: " + slug);
        }

        return Map.of(
                "url", gatewayBaseUrl + "/docs/" + slug + "/v3/api-docs"
        );
    }

    @GetMapping(value = "/{slug}/swagger-ui", produces = MediaType.TEXT_HTML_VALUE)
    public String serviceSwaggerUi(@PathVariable String slug) {
        Map<String, String> service = serviceLinks().stream()
                .filter(link -> slug.equals(link.get("slug")))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown docs service: " + slug));

        String title = escapeHtml(service.get("name"));
        String apiDocsUrl = escapeJs(gatewayBaseUrl + "/docs/" + slug + "/v3/api-docs");

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s - Swagger UI</title>
                  <link rel="stylesheet" type="text/css" href="%s/webjars/swagger-ui/swagger-ui.css" />
                  <style>
                    html { box-sizing: border-box; overflow-y: scroll; }
                    *, *:before, *:after { box-sizing: inherit; }
                    body { margin: 0; background: #fafafa; }
                  </style>
                </head>
                <body>
                  <div id="swagger-ui"></div>
                  <script src="%s/webjars/swagger-ui/swagger-ui-bundle.js"></script>
                  <script src="%s/webjars/swagger-ui/swagger-ui-standalone-preset.js"></script>
                  <script>
                    window.onload = function() {
                      window.ui = SwaggerUIBundle({
                        url: "%s",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        layout: "StandaloneLayout"
                      });
                    };
                  </script>
                </body>
                </html>
                """.formatted(title, gatewayBaseUrl, gatewayBaseUrl, gatewayBaseUrl, apiDocsUrl);
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
                "slug", slug,
                "swaggerUi", gatewayBaseUrl + "/docs/" + slug + "/swagger-ui",
                "apiDocs", apiDocsUrl
        );
    }

    private String encode(String value) {
        return value.replace(" ", "%20");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeJs(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
