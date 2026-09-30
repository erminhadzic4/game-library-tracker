package com.erminhadzic.gamelibrarytracker.controller;

import com.erminhadzic.gamelibrarytracker.dto.AddLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.dto.LibraryEntryResponse;
import com.erminhadzic.gamelibrarytracker.dto.LibraryStatsResponse;
import com.erminhadzic.gamelibrarytracker.dto.UpdateLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.service.LibraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Every method takes the user from the JWT (via @AuthenticationPrincipal), never from the request body
@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    public ResponseEntity<LibraryEntryResponse> add(@AuthenticationPrincipal UserDetails user,
                                                    @RequestBody AddLibraryEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.addEntry(user.getUsername(), request));
    }

    @GetMapping
    public List<LibraryEntryResponse> list(@AuthenticationPrincipal UserDetails user,
                                           @RequestParam(required = false) LibraryEntry.Status status) {
        return libraryService.getEntries(user.getUsername(), status);
    }

    @GetMapping("/stats")
    public LibraryStatsResponse stats(@AuthenticationPrincipal UserDetails user) {
        return libraryService.getStats(user.getUsername());
    }

    @PatchMapping("/{id}")
    public LibraryEntryResponse update(@AuthenticationPrincipal UserDetails user,
                                       @PathVariable Long id,
                                       @RequestBody UpdateLibraryEntryRequest request) {
        return libraryService.updateEntry(user.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        libraryService.deleteEntry(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
