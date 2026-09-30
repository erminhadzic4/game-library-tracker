package com.erminhadzic.gamelibrarytracker.dto;

// averageRating is rounded to one decimal place, and null when none of the user's entries has a rating
public record LibraryStatsResponse(
        long total,
        long playing,
        long backlog,
        long completed,
        Double averageRating) {
}
