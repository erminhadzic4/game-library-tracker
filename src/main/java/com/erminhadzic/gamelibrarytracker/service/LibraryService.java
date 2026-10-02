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
import org.springframework.dao.DataIntegrityViolationException;
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
        // Required fields and lengths are validated on AddLibraryEntryRequest (@Valid in the controller)
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
        try {
            // saveAndFlush sends the INSERT now, so a unique-constraint violation shows up here and not at commit
            return LibraryEntryResponse.from(libraryEntryRepository.saveAndFlush(entry));
        } catch (DataIntegrityViolationException e) {
            // Two requests added the same game at the same moment: both passed the check above, and the
            // unique constraint on (user_id, game_id) rejected the second INSERT. The user and game exist and
            // status is set, so that constraint is the only one this INSERT can break.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This game is already in your library");
        }
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
        // status required, rating 1-10 and the notes length are validated on UpdateLibraryEntryRequest
        LibraryEntry entry = getOwnedEntry(username, entryId);

        // Full replacement: all three fields are overwritten, so a null rating or notes clears the saved value
        entry.setStatus(request.status());
        entry.setRating(request.rating());
        // Blank or whitespace-only notes are stored as null, so "no notes" has one representation
        String notes = (request.notes() == null) ? null : request.notes().trim();
        entry.setNotes((notes == null || notes.isEmpty()) ? null : notes);
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
