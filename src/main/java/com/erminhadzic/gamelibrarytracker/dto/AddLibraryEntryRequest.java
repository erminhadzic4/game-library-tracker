package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// Game fields come from a RAWG search result, so the client can add a game without a second RAWG call.
// 255 is the length of the title and cover_image_url columns.
public record AddLibraryEntryRequest(
        @Schema(description = "The game's id in the RAWG API (\"id\" in a search result)", example = "3328")
        @NotNull(message = "rawgId is required")
        Long rawgId,

        @Schema(description = "\"name\" in a RAWG search result", example = "The Witcher 3: Wild Hunt")
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters long")
        String title,

        @Schema(description = "\"background_image\" in a RAWG search result", nullable = true)
        @Size(max = 255, message = "coverImageUrl must be at most 255 characters long")
        String coverImageUrl,

        @Schema(description = "\"released\" in a RAWG search result", example = "2015-05-18", nullable = true)
        LocalDate releaseDate,

        @NotNull(message = "status is required")
        LibraryEntry.Status status) {
}
