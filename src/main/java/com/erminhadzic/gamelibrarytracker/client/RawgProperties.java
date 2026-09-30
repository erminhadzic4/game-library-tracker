package com.erminhadzic.gamelibrarytracker.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Binds rawg.* from application.properties. Like JwtProperties, @Validated checks the values at startup, so a missing
// RAWG_API_KEY stops the app with an "APPLICATION FAILED TO START" report naming the property, instead of a stack trace
@Validated
@ConfigurationProperties(prefix = "rawg")
public record RawgProperties(
        @NotBlank
        String baseUrl,
        @NotBlank(message = "must be set via the RAWG_API_KEY environment variable (get a free key at https://rawg.io/apidocs)")
        String apiKey) {
}
