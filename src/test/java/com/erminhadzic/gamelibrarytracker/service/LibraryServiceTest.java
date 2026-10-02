package com.erminhadzic.gamelibrarytracker.service;

import com.erminhadzic.gamelibrarytracker.dto.AddLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.dto.LibraryEntryResponse;
import com.erminhadzic.gamelibrarytracker.dto.LibraryStatsResponse;
import com.erminhadzic.gamelibrarytracker.dto.UpdateLibraryEntryRequest;
import com.erminhadzic.gamelibrarytracker.model.Game;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry.Status;
import com.erminhadzic.gamelibrarytracker.model.User;
import com.erminhadzic.gamelibrarytracker.repository.GameRepository;
import com.erminhadzic.gamelibrarytracker.repository.LibraryEntryRepository;
import com.erminhadzic.gamelibrarytracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Pure unit tests: all three repositories are Mockito mocks, so no Spring context or database
@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LibraryEntryRepository libraryEntryRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LibraryService libraryService;

    private User alice;
    private User bob;
    private Game witcher;

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        witcher = new Game();
        witcher.setId(10L);
        witcher.setRawgId(3328L);
        witcher.setTitle("The Witcher 3: Wild Hunt");
    }

    // --- addEntry ---

    @Test
    void addEntry_newGame_createsGameAndEntry() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(gameRepository.findByRawgId(3328L)).thenReturn(Optional.empty());
        // Simulate the database assigning ids on save
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game game = inv.getArgument(0);
            game.setId(10L);
            return game;
        });
        when(libraryEntryRepository.findByUserAndGame_Id(alice, 10L)).thenReturn(Optional.empty());
        when(libraryEntryRepository.save(any(LibraryEntry.class))).thenAnswer(inv -> {
            LibraryEntry entry = inv.getArgument(0);
            entry.setId(100L);
            return entry;
        });

        LibraryEntryResponse response = libraryService.addEntry("alice", new AddLibraryEntryRequest(
                3328L, "The Witcher 3: Wild Hunt", "https://img/w3.jpg", LocalDate.of(2015, 5, 18), Status.PLAYING));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.gameId()).isEqualTo(10L);
        assertThat(response.rawgId()).isEqualTo(3328L);
        assertThat(response.title()).isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(response.status()).isEqualTo(Status.PLAYING);
        assertThat(response.rating()).isNull();
    }

    @Test
    void addEntry_existingGame_reusesGameRow() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(gameRepository.findByRawgId(3328L)).thenReturn(Optional.of(witcher));
        when(libraryEntryRepository.findByUserAndGame_Id(alice, 10L)).thenReturn(Optional.empty());
        when(libraryEntryRepository.save(any(LibraryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        LibraryEntryResponse response = libraryService.addEntry("alice",
                new AddLibraryEntryRequest(3328L, "The Witcher 3: Wild Hunt", null, null, Status.BACKLOG));

        assertThat(response.gameId()).isEqualTo(10L);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void addEntry_gameAlreadyInLibrary_returns409() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(gameRepository.findByRawgId(3328L)).thenReturn(Optional.of(witcher));
        when(libraryEntryRepository.findByUserAndGame_Id(alice, 10L))
                .thenReturn(Optional.of(entry(100L, alice, Status.PLAYING, null)));

        assertStatus(() -> libraryService.addEntry("alice",
                        new AddLibraryEntryRequest(3328L, "The Witcher 3: Wild Hunt", null, null, Status.BACKLOG)),
                HttpStatus.CONFLICT);
        verify(libraryEntryRepository, never()).save(any());
    }

    @Test
    void addEntry_missingFields_returns400() {
        assertStatus(() -> libraryService.addEntry("alice",
                new AddLibraryEntryRequest(null, "Title", null, null, Status.PLAYING)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> libraryService.addEntry("alice",
                new AddLibraryEntryRequest(3328L, " ", null, null, Status.PLAYING)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> libraryService.addEntry("alice",
                new AddLibraryEntryRequest(3328L, "Title", null, null, null)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository, gameRepository, libraryEntryRepository);
    }

    // --- updateEntry ---

    @Test
    void updateEntry_ownEntry_replacesAllEditableFields() {
        LibraryEntry entry = entry(100L, alice, Status.PLAYING, null);
        entry.setNotes("old notes");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.findByIdAndUser(100L, alice)).thenReturn(Optional.of(entry));

        LibraryEntryResponse response = libraryService.updateEntry("alice", 100L,
                new UpdateLibraryEntryRequest(Status.COMPLETED, 9, "  new notes  "));

        assertThat(response.status()).isEqualTo(Status.COMPLETED);
        assertThat(response.rating()).isEqualTo(9);
        // notes are trimmed before they are stored
        assertThat(response.notes()).isEqualTo("new notes");
    }

    @Test
    void updateEntry_nullRatingAndNotes_clearsThem() {
        LibraryEntry entry = entry(100L, alice, Status.PLAYING, 8);
        entry.setNotes("old notes");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.findByIdAndUser(100L, alice)).thenReturn(Optional.of(entry));

        LibraryEntryResponse response = libraryService.updateEntry("alice", 100L,
                new UpdateLibraryEntryRequest(Status.PLAYING, null, null));

        assertThat(response.status()).isEqualTo(Status.PLAYING);
        assertThat(response.rating()).isNull();
        assertThat(response.notes()).isNull();
        // The entity itself was changed, which is what Hibernate writes when the transaction commits
        assertThat(entry.getRating()).isNull();
        assertThat(entry.getNotes()).isNull();
    }

    @Test
    void updateEntry_blankNotes_storedAsNull() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        for (String blank : new String[] {"", "   ", " \n\t "}) {
            LibraryEntry entry = entry(100L, alice, Status.PLAYING, 8);
            entry.setNotes("old notes");
            when(libraryEntryRepository.findByIdAndUser(100L, alice)).thenReturn(Optional.of(entry));

            LibraryEntryResponse response = libraryService.updateEntry("alice", 100L,
                    new UpdateLibraryEntryRequest(Status.PLAYING, 8, blank));

            assertThat(response.notes()).isNull();
            assertThat(response.rating()).isEqualTo(8);
        }
    }

    @Test
    void updateEntry_missingStatus_returns400() {
        assertStatus(() -> libraryService.updateEntry("alice", 100L, new UpdateLibraryEntryRequest(null, 8, "notes")),
                HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository, libraryEntryRepository);
    }

    @Test
    void updateEntry_ratingOutOfRange_returns400() {
        assertStatus(() -> libraryService.updateEntry("alice", 100L,
                new UpdateLibraryEntryRequest(Status.PLAYING, 0, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> libraryService.updateEntry("alice", 100L,
                new UpdateLibraryEntryRequest(Status.PLAYING, 11, null)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(libraryEntryRepository);
    }

    @Test
    void updateEntry_entryDoesNotExist_returns404() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.findByIdAndUser(999L, alice)).thenReturn(Optional.empty());

        assertStatus(() -> libraryService.updateEntry("alice", 999L, new UpdateLibraryEntryRequest(Status.PLAYING, 8, null)),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void updateEntry_anotherUsersEntry_returns404() {
        // Entry 100 belongs to alice; the ownership-aware query finds nothing for bob
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(libraryEntryRepository.findByIdAndUser(100L, bob)).thenReturn(Optional.empty());

        assertStatus(() -> libraryService.updateEntry("bob", 100L, new UpdateLibraryEntryRequest(Status.PLAYING, 1, null)),
                HttpStatus.NOT_FOUND);
    }

    // --- deleteEntry ---

    @Test
    void deleteEntry_ownEntry_deletesIt() {
        LibraryEntry entry = entry(100L, alice, Status.PLAYING, null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.findByIdAndUser(100L, alice)).thenReturn(Optional.of(entry));

        libraryService.deleteEntry("alice", 100L);

        verify(libraryEntryRepository).delete(entry);
    }

    @Test
    void deleteEntry_entryDoesNotExist_returns404() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.findByIdAndUser(999L, alice)).thenReturn(Optional.empty());

        assertStatus(() -> libraryService.deleteEntry("alice", 999L), HttpStatus.NOT_FOUND);
        verify(libraryEntryRepository, never()).delete(any());
    }

    @Test
    void deleteEntry_anotherUsersEntry_returns404() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(libraryEntryRepository.findByIdAndUser(100L, bob)).thenReturn(Optional.empty());

        assertStatus(() -> libraryService.deleteEntry("bob", 100L), HttpStatus.NOT_FOUND);
        verify(libraryEntryRepository, never()).delete(any());
    }

    // --- getStats ---

    @Test
    void getStats_returnsCountsTotalAndRoundedAverage() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.countByUserAndStatus(alice, Status.PLAYING)).thenReturn(2L);
        when(libraryEntryRepository.countByUserAndStatus(alice, Status.BACKLOG)).thenReturn(3L);
        when(libraryEntryRepository.countByUserAndStatus(alice, Status.COMPLETED)).thenReturn(1L);
        // e.g. ratings 9, 8, 8
        when(libraryEntryRepository.findAverageRatingByUser(alice)).thenReturn(25 / 3.0);

        LibraryStatsResponse stats = libraryService.getStats("alice");

        assertThat(stats.playing()).isEqualTo(2);
        assertThat(stats.backlog()).isEqualTo(3);
        assertThat(stats.completed()).isEqualTo(1);
        assertThat(stats.total()).isEqualTo(6);
        assertThat(stats.averageRating()).isEqualTo(8.3);
    }

    @Test
    void getStats_nothingRated_averageIsNull() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(libraryEntryRepository.countByUserAndStatus(alice, Status.PLAYING)).thenReturn(1L);
        when(libraryEntryRepository.countByUserAndStatus(alice, Status.BACKLOG)).thenReturn(0L);
        when(libraryEntryRepository.countByUserAndStatus(alice, Status.COMPLETED)).thenReturn(0L);
        when(libraryEntryRepository.findAverageRatingByUser(alice)).thenReturn(null);

        LibraryStatsResponse stats = libraryService.getStats("alice");

        assertThat(stats.total()).isEqualTo(1);
        assertThat(stats.averageRating()).isNull();
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private LibraryEntry entry(Long id, User owner, Status status, Integer rating) {
        LibraryEntry entry = new LibraryEntry();
        entry.setId(id);
        entry.setUser(owner);
        entry.setGame(witcher);
        entry.setStatus(status);
        entry.setRating(rating);
        return entry;
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
