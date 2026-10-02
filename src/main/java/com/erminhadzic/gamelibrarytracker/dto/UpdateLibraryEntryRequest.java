package com.erminhadzic.gamelibrarytracker.dto;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Full replacement of the editable fields (PUT): status is required; a null rating or notes clears the saved value
public record UpdateLibraryEntryRequest(
        @NotNull(message = "status is required")
        LibraryEntry.Status status,

        // @Min and @Max accept null, so "not rated" stays valid
        @Min(value = 1, message = "rating must be between 1 and 10")
        @Max(value = 10, message = "rating must be between 1 and 10")
        Integer rating,

        // The notes column is TEXT (no database limit), so this is an application limit
        @Size(max = 2000, message = "notes must be at most 2000 characters long")
        String notes) {
}
