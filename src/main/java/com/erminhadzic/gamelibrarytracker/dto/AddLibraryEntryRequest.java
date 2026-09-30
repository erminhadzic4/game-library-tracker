package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import java.time.LocalDate;

// Game fields come from a RAWG search result, so the client can add a game without a second RAWG call
public record AddLibraryEntryRequest(
        Long rawgId,
        String title,
        String coverImageUrl,
        LocalDate releaseDate,
        LibraryEntry.Status status) {
}
