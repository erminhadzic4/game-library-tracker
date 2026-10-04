package com.erminhadzic.gamelibrarytracker.controller;

import com.erminhadzic.gamelibrarytracker.config.OpenApiConfig;
import com.erminhadzic.gamelibrarytracker.dto.AddLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.dto.LibraryEntryResponse;
import com.erminhadzic.gamelibrarytracker.dto.LibraryStatsResponse;
import com.erminhadzic.gamelibrarytracker.dto.UpdateLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Every method takes the user from the JWT (via @AuthenticationPrincipal), never from the request body
@RestController
@RequestMapping("/api/library")
@Tag(name = "Library", description = "The logged-in user's own game library")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Operation(summary = "Add a game to the library")
    @ApiResponse(responseCode = "201", description = "Entry created")
    @ApiResponse(responseCode = "400", description = "A field is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The game is already in the library",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<LibraryEntryResponse> add(@AuthenticationPrincipal UserDetails user,
                                                    @Valid @RequestBody AddLibraryEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.addEntry(user.getUsername(), request));
    }

    @Operation(summary = "List the library entries, newest first")
    @ApiResponse(responseCode = "200", description = "The user's entries (an empty list if there are none)")
    @ApiResponse(responseCode = "400", description = "status is not a known value",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public List<LibraryEntryResponse> list(@AuthenticationPrincipal UserDetails user,
                                           @Parameter(description = "Only return entries with this status")
                                           @RequestParam(required = false) LibraryEntry.Status status) {
        return libraryService.getEntries(user.getUsername(), status);
    }

    @Operation(summary = "Get the library's counts per status and the average rating")
    @ApiResponse(responseCode = "200", description = "The statistics")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/stats")
    public LibraryStatsResponse stats(@AuthenticationPrincipal UserDetails user) {
        return libraryService.getStats(user.getUsername());
    }

    // PUT, not PATCH: the request replaces all editable fields (status, rating, notes) at once
    @Operation(summary = "Replace an entry's status, rating and notes")
    @ApiResponse(responseCode = "200", description = "The updated entry")
    @ApiResponse(responseCode = "400", description = "A field is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such entry in this user's library",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public LibraryEntryResponse update(@AuthenticationPrincipal UserDetails user,
                                       @Parameter(description = "Id of the library entry") @PathVariable Long id,
                                       @Valid @RequestBody UpdateLibraryEntryRequest request) {
        return libraryService.updateEntry(user.getUsername(), id, request);
    }

    @Operation(summary = "Remove an entry from the library")
    @ApiResponse(responseCode = "204", description = "Entry removed")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such entry in this user's library",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user,
                                       @Parameter(description = "Id of the library entry") @PathVariable Long id) {
        libraryService.deleteEntry(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
