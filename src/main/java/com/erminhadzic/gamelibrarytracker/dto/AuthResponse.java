package com.erminhadzic.gamelibrarytracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT, valid for 24 hours. Send it as \"Authorization: Bearer <token>\".")
        String token) {
}
