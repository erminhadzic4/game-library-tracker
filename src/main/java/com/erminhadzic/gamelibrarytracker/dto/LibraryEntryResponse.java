package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.Game;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Flattened view of an entry and its game; never exposes the User entity (which holds the password hash)
public record LibraryEntryResponse(
        @Schema(description = "Id of the library entry; used in PUT and DELETE /api/library/{id}")
        Long id,
        @Schema(description = "Id of the game in this application's database")
        Long gameId,
        @Schema(description = "Id of the game in the RAWG API")
        Long rawgId,
        String title,
        @Schema(nullable = true)
        String coverImageUrl,
        @Schema(nullable = true)
        LocalDate releaseDate,
        LibraryEntry.Status status,
        @Schema(description = "1 to 10; null when not rated", nullable = true)
        Integer rating,
        @Schema(nullable = true)
        String notes,
        @Schema(description = "When the game was added to the library")
        LocalDateTime addedAt) {

    public static LibraryEntryResponse from(LibraryEntry entry) {
        Game game = entry.getGame();
        return new LibraryEntryResponse(
                entry.getId(),
                game.getId(),
                game.getRawgId(),
                game.getTitle(),
                game.getCoverImageUrl(),
                game.getReleaseDate(),
                entry.getStatus(),
                entry.getRating(),
                entry.getNotes(),
                entry.getAddedAt());
    }
}
