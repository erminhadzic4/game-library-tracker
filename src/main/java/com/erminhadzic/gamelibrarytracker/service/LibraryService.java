package com.erminhadzic.gamelibrarytracker.service;

import com.erminhadzic.gamelibrarytracker.dto.AddLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.dto.LibraryEntryResponse;
import com.erminhadzic.gamelibrarytracker.dto.LibraryStatsResponse;
import com.erminhadzic.gamelibrarytracker.dto.UpdateLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.model.Game;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.model.User;
import com.erminhadzic.gamelibrarytracker.repository.GameRepository;
import com.erminhadzic.gamelibrarytracker.repository.LibraryEntryRepository;
import com.erminhadzic.gamelibrarytracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class LibraryService {

    private final LibraryEntryRepository libraryEntryRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public LibraryService(LibraryEntryRepository libraryEntryRepository,
                          GameRepository gameRepository,
                          UserRepository userRepository) {
        this.libraryEntryRepository = libraryEntryRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LibraryEntryResponse addEntry(String username, AddLibraryEntryRequest request) {
        if (request.rawgId() == null || request.title() == null || request.title().isBlank() || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rawgId, title and status are required");
        }
        User user = getUser(username);

        // Games are a shared cache: reuse the row if any user has added this RAWG game before
        Game game = gameRepository.findByRawgId(request.rawgId())
                .orElseGet(() -> {
                    Game newGame = new Game();
                    newGame.setRawgId(request.rawgId());
                    newGame.setTitle(request.title());
                    newGame.setCoverImageUrl(request.coverImageUrl());
                    newGame.setReleaseDate(request.releaseDate());
                    return gameRepository.save(newGame);
                });

        if (libraryEntryRepository.findByUserAndGame_Id(user, game.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This game is already in your library");
        }

        LibraryEntry entry = new LibraryEntry();
        entry.setUser(user);
        entry.setGame(game);
        entry.setStatus(request.status());
        return LibraryEntryResponse.from(libraryEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<LibraryEntryResponse> getEntries(String username, LibraryEntry.Status status) {
        User user = getUser(username);
        List<LibraryEntry> entries = (status == null)
                ? libraryEntryRepository.findByUserOrderByAddedAtDesc(user)
                : libraryEntryRepository.findByUserAndStatusOrderByAddedAtDesc(user, status);
        return entries.stream().map(LibraryEntryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LibraryStatsResponse getStats(String username) {
        User user = getUser(username);
        long playing = libraryEntryRepository.countByUserAndStatus(user, LibraryEntry.Status.PLAYING);
        long backlog = libraryEntryRepository.countByUserAndStatus(user, LibraryEntry.Status.BACKLOG);
        long completed = libraryEntryRepository.countByUserAndStatus(user, LibraryEntry.Status.COMPLETED);
        Double average = libraryEntryRepository.findAverageRatingByUser(user);
        // Round to one decimal place (e.g. 8.333 -> 8.3); stays null when nothing is rated
        Double roundedAverage = (average == null) ? null : Math.round(average * 10) / 10.0;
        return new LibraryStatsResponse(
                playing + backlog + completed,
                playing,
                backlog,
                completed,
                roundedAverage);
    }

    @Transactional
    public LibraryEntryResponse updateEntry(String username, Long entryId, UpdateLibraryEntryRequest request) {
        if (request.status() == null && request.rating() == null && request.notes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one of status, rating or notes");
        }
        if (request.rating() != null && (request.rating() < 1 || request.rating() > 10)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be between 1 and 10");
        }
        LibraryEntry entry = getOwnedEntry(username, entryId);

        if (request.status() != null) {
            entry.setStatus(request.status());
        }
        if (request.rating() != null) {
            entry.setRating(request.rating());
        }
        if (request.notes() != null) {
            entry.setNotes(request.notes());
        }
        // No save() needed: the entry is managed, so Hibernate writes the changes when the transaction commits
        return LibraryEntryResponse.from(entry);
    }

    @Transactional
    public void deleteEntry(String username, Long entryId) {
        libraryEntryRepository.delete(getOwnedEntry(username, entryId));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // 404 (not 403) for another user's entry, so callers can't tell which entry ids exist
    private LibraryEntry getOwnedEntry(String username, Long entryId) {
        return libraryEntryRepository.findByIdAndUser(entryId, getUser(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library entry not found"));
    }
}
