package com.insureai.insurance.adapter.inbound.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {
    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI quoteServiceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Quote Service API")
                .version("v1")
                .description("Creates and retrieves insurance quotes. Requires a Keycloak-issued JWT carrying the "
                    + "quote-service audience and the scope listed on each operation."))
            .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
