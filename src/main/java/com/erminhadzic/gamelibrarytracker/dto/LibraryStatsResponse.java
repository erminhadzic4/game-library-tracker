package com.erminhadzic.gamelibrarytracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// averageRating is rounded to one decimal place, and null when none of the user's entries has a rating
public record LibraryStatsResponse(
        @Schema(description = "Number of entries in the library")
        long total,
        long playing,
        long backlog,
        long completed,
        @Schema(description = "Average of the rated entries, one decimal place; null when nothing is rated",
                example = "7.5", nullable = true)
        Double averageRating) {
}
