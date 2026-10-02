package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;

// Full replacement of the editable fields (PUT): status is required; a null rating or notes clears the saved value
public record UpdateLibraryEntryRequest(
        LibraryEntry.Status status,
        Integer rating,
        String notes) {
}
