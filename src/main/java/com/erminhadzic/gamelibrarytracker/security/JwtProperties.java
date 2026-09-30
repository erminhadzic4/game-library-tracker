package com.erminhadzic.gamelibrarytracker.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Binds jwt.* from application.properties. @Validated checks the values at startup, and if they are invalid
// Spring Boot stops with an "APPLICATION FAILED TO START" report naming the property, instead of a long stack trace
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "must be set via the JWT_SECRET environment variable (a Base64-encoded key of at least 256 bits)")
        String secret,
        @Positive
        long expirationMs) {
}
