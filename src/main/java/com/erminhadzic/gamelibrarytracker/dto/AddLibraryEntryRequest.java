package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// Game fields come from a RAWG search result, so the client can add a game without a second RAWG call.
// 255 is the length of the title and cover_image_url columns.
public record AddLibraryEntryRequest(
        @NotNull(message = "rawgId is required")
        Long rawgId,

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters long")
        String title,

        @Size(max = 255, message = "coverImageUrl must be at most 255 characters long")
        String coverImageUrl,

        LocalDate releaseDate,

        @NotNull(message = "status is required")
        LibraryEntry.Status status) {
}
