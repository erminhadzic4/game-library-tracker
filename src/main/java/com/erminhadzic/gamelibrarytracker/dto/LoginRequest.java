package com.erminhadzic.gamelibrarytracker.dto;

import jakarta.validation.constraints.NotBlank;

// Only "required" here: the length rules belong to registration, and existing accounts must still be able to log in
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password) {
}
