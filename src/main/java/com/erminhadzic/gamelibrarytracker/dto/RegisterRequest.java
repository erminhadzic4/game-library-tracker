package com.erminhadzic.gamelibrarytracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The rules are checked by @Valid in AuthController, before the service is called
public record RegisterRequest(
        @Schema(example = "ermin")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters long")
        String username,

        // 255 is the length of the email column
        @Schema(example = "ermin@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters long")
        String email,

        // BCrypt only uses the first 72 bytes of a password, so longer ones are rejected instead of silently cut
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters long")
        String password) {
}
