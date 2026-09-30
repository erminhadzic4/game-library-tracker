package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.Game;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Flattened view of an entry and its game; never exposes the User entity (which holds the password hash)
public record LibraryEntryResponse(
        Long id,
        Long gameId,
        Long rawgId,
        String title,
        String coverImageUrl,
        LocalDate releaseDate,
        LibraryEntry.Status status,
        Integer rating,
        String notes,
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
