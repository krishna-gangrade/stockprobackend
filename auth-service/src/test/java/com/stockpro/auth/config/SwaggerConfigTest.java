package com.stockpro.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SwaggerConfig unit tests")
class SwaggerConfigTest {

    @Test
    void authServiceOpenAPI_buildsExpectedMetadata() {
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 9090);

        OpenAPI openAPI = config.authServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("StockPro — Auth Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("platform@stockpro.com");
        assertThat(openAPI.getServers()).singleElement().extracting(io.swagger.v3.oas.models.servers.Server::getUrl)
                .isEqualTo("http://localhost:9090/api/v1");
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("BearerAuth");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("BearerAuth").getBearerFormat())
                .isEqualTo("JWT");
    }
}
