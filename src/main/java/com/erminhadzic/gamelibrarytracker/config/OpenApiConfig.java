package com.erminhadzic.gamelibrarytracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The general part of the OpenAPI description that springdoc serves at /v3/api-docs and shows in Swagger UI
// (/swagger-ui.html). The endpoints themselves are read from the controllers and their annotations.
@Configuration
public class OpenApiConfig {

    // Name of the security scheme below; the controllers refer to it in @SecurityRequirement
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI gameLibraryTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Game Library Tracker API")
                        .description("REST API for tracking a personal video game library: search games (RAWG), "
                                + "add them to a library, set a status, rate them and keep notes.")
                        .version("1.0.0"))
                // "Authorization: Bearer <token>" header. This is what puts the "Authorize" button into Swagger UI.
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("The token returned by /api/auth/login or /api/auth/register")))
                // Every endpoint needs the token by default; the two auth endpoints opt out with @SecurityRequirements
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
