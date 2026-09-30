package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;

// Partial update: a null field means "leave unchanged"
public record UpdateLibraryEntryRequest(
        LibraryEntry.Status status,
        Integer rating,
        String notes) {
}
