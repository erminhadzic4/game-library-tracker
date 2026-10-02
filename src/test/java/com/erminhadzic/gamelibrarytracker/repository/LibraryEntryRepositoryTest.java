package com.erminhadzic.gamelibrarytracker.repository;

import com.erminhadzic.gamelibrarytracker.model.Game;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.model.LibraryEntry.Status;
import com.erminhadzic.gamelibrarytracker.model.User;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Repository slice test: only the JPA part of the application is started (entities, repositories, Hibernate, H2),
// so the queries run as real SQL. Each test runs in a transaction that is rolled back afterwards,
// so the tests don't see each other's rows.
// The "test" profile is only needed for the dummy jwt/rawg settings that the application class validates at startup.
@DataJpaTest
@ActiveProfiles("test")
class LibraryEntryRepositoryTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 1, 5, 12, 0);
    private static final LocalDateTime TUESDAY = MONDAY.plusDays(1);
    private static final LocalDateTime WEDNESDAY = MONDAY.plusDays(2);

    @Autowired
    private LibraryEntryRepository libraryEntryRepository;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private Game witcher;
    private Game portal;
    private Game hades;

    @BeforeEach
    void setUp() {
        alice = saveUser("alice");
        bob = saveUser("bob");
        witcher = saveGame(3328L, "The Witcher 3");
        portal = saveGame(4200L, "Portal 2");
        hades = saveGame(274755L, "Hades");
    }

    // --- findByUserOrderByAddedAtDesc ---

    @Test
    void findByUser_returnsNewestFirst() {
        // Saved in a different order than the dates, so the result can't be right by accident (e.g. by id)
        saveEntry(alice, witcher, Status.PLAYING, null, TUESDAY);
        saveEntry(alice, portal, Status.BACKLOG, null, WEDNESDAY);
        saveEntry(alice, hades, Status.COMPLETED, null, MONDAY);
        flushAndClear();

        List<LibraryEntry> entries = libraryEntryRepository.findByUserOrderByAddedAtDesc(alice);

        assertThat(titles(entries)).containsExactly("Portal 2", "The Witcher 3", "Hades");
    }

    @Test
    void findByUser_returnsOnlyThatUsersEntries() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        saveEntry(bob, witcher, Status.PLAYING, null, TUESDAY);
        saveEntry(bob, portal, Status.BACKLOG, null, WEDNESDAY);
        flushAndClear();

        List<LibraryEntry> entries = libraryEntryRepository.findByUserOrderByAddedAtDesc(alice);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getUser().getId()).isEqualTo(alice.getId());
        assertThat(titles(entries)).containsExactly("The Witcher 3");
    }

    @Test
    void findByUser_noEntries_returnsEmptyList() {
        saveEntry(bob, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findByUserOrderByAddedAtDesc(alice)).isEmpty();
    }

    // --- findByUserAndStatusOrderByAddedAtDesc ---

    @Test
    void findByUserAndStatus_filtersByStatusNewestFirst() {
        saveEntry(alice, witcher, Status.BACKLOG, null, MONDAY);
        saveEntry(alice, portal, Status.PLAYING, null, TUESDAY);
        saveEntry(alice, hades, Status.BACKLOG, null, WEDNESDAY);
        flushAndClear();

        List<LibraryEntry> backlog = libraryEntryRepository.findByUserAndStatusOrderByAddedAtDesc(alice, Status.BACKLOG);

        assertThat(titles(backlog)).containsExactly("Hades", "The Witcher 3");
    }

    @Test
    void findByUserAndStatus_ignoresOtherUsersEntriesWithTheSameStatus() {
        saveEntry(alice, witcher, Status.BACKLOG, null, MONDAY);
        saveEntry(bob, portal, Status.BACKLOG, null, TUESDAY);
        flushAndClear();

        List<LibraryEntry> backlog = libraryEntryRepository.findByUserAndStatusOrderByAddedAtDesc(alice, Status.BACKLOG);

        assertThat(titles(backlog)).containsExactly("The Witcher 3");
    }

    // --- findByUserAndGame_Id ---

    @Test
    void findByUserAndGameId_foundForTheOwner() {
        LibraryEntry entry = saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findByUserAndGame_Id(alice, witcher.getId()))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(entry.getId()));
    }

    @Test
    void findByUserAndGameId_emptyForAUserWhoDoesNotHaveTheGame() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findByUserAndGame_Id(bob, witcher.getId())).isEmpty();
    }

    // --- findByIdAndUser: the ownership check behind the 404 for another user's entry ---

    @Test
    void findByIdAndUser_foundForTheOwner() {
        LibraryEntry entry = saveEntry(alice, witcher, Status.PLAYING, 9, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findByIdAndUser(entry.getId(), alice))
                .hasValueSatisfying(found -> assertThat(found.getRating()).isEqualTo(9));
    }

    @Test
    void findByIdAndUser_emptyForAnotherUser() {
        LibraryEntry entry = saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findByIdAndUser(entry.getId(), bob)).isEmpty();
    }

    // --- countByUserAndStatus ---

    @Test
    void countByUserAndStatus_countsOnlyThatUserAndStatus() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        saveEntry(alice, portal, Status.PLAYING, null, TUESDAY);
        saveEntry(alice, hades, Status.BACKLOG, null, WEDNESDAY);
        saveEntry(bob, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.countByUserAndStatus(alice, Status.PLAYING)).isEqualTo(2);
        assertThat(libraryEntryRepository.countByUserAndStatus(alice, Status.BACKLOG)).isEqualTo(1);
        assertThat(libraryEntryRepository.countByUserAndStatus(bob, Status.PLAYING)).isEqualTo(1);
    }

    @Test
    void countByUserAndStatus_isZeroWhenThereAreNone() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.countByUserAndStatus(alice, Status.COMPLETED)).isZero();
        assertThat(libraryEntryRepository.countByUserAndStatus(bob, Status.PLAYING)).isZero();
    }

    // --- findAverageRatingByUser (JPQL AVG) ---

    @Test
    void averageRating_ignoresEntriesWithoutARating() {
        saveEntry(alice, witcher, Status.COMPLETED, 7, MONDAY);
        saveEntry(alice, portal, Status.COMPLETED, 8, TUESDAY);
        saveEntry(alice, hades, Status.BACKLOG, null, WEDNESDAY);
        flushAndClear();

        // (7 + 8) / 2: the unrated entry is not counted as 0 and not counted in the divisor
        assertThat(libraryEntryRepository.findAverageRatingByUser(alice)).isEqualTo(7.5);
    }

    @Test
    void averageRating_onlyAveragesThatUsersEntries() {
        saveEntry(alice, witcher, Status.COMPLETED, 10, MONDAY);
        saveEntry(bob, witcher, Status.COMPLETED, 1, MONDAY);
        saveEntry(bob, portal, Status.COMPLETED, 2, TUESDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findAverageRatingByUser(alice)).isEqualTo(10.0);
        assertThat(libraryEntryRepository.findAverageRatingByUser(bob)).isEqualTo(1.5);
    }

    @Test
    void averageRating_isNullWhenNothingIsRated() {
        saveEntry(alice, witcher, Status.BACKLOG, null, MONDAY);
        saveEntry(alice, portal, Status.PLAYING, null, TUESDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findAverageRatingByUser(alice)).isNull();
    }

    @Test
    void averageRating_isNullWhenTheUserHasNoEntries() {
        saveEntry(bob, witcher, Status.COMPLETED, 9, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.findAverageRatingByUser(alice)).isNull();
    }

    // --- @EntityGraph: is the game loaded together with the entry? ---

    @Test
    void listQueries_loadTheGameInTheSameQuery() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        // Clearing matters here: otherwise the game would still be in Hibernate's first-level cache
        // and would look "loaded" whatever the query did
        flushAndClear();

        LibraryEntry fromList = libraryEntryRepository.findByUserOrderByAddedAtDesc(alice).get(0);
        assertThat(isLoaded(fromList.getGame())).isTrue();

        em.clear();
        LibraryEntry fromStatusList =
                libraryEntryRepository.findByUserAndStatusOrderByAddedAtDesc(alice, Status.PLAYING).get(0);
        assertThat(isLoaded(fromStatusList.getGame())).isTrue();
    }

    // findByIdAndUser has no @EntityGraph, so the game is a lazy proxy. Reading it needs an open session,
    // which is why LibraryService.updateEntry builds its response inside the transaction (open-in-view is off).
    @Test
    void findByIdAndUser_returnsTheGameAsALazyProxy() {
        LibraryEntry entry = saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        flushAndClear();

        LibraryEntry found = libraryEntryRepository.findByIdAndUser(entry.getId(), alice).orElseThrow();

        assertThat(isLoaded(found.getGame())).isFalse();
        // Inside the transaction the proxy loads itself on first use
        assertThat(found.getGame().getTitle()).isEqualTo("The Witcher 3");
        assertThat(isLoaded(found.getGame())).isTrue();
    }

    // --- unique constraint on (user_id, game_id) ---

    // LibraryService.addEntry relies on this exception to answer 409 when two requests add the same game at once
    @Test
    void savingTheSameGameTwiceForOneUser_throwsDataIntegrityViolation() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);

        LibraryEntry duplicate = new LibraryEntry();
        duplicate.setUser(alice);
        duplicate.setGame(witcher);
        duplicate.setStatus(Status.BACKLOG);

        assertThatThrownBy(() -> libraryEntryRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void twoUsersCanHaveTheSameGame() {
        saveEntry(alice, witcher, Status.PLAYING, null, MONDAY);
        saveEntry(bob, witcher, Status.BACKLOG, null, MONDAY);
        flushAndClear();

        assertThat(libraryEntryRepository.count()).isEqualTo(2);
    }

    // --- helpers ---

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPassword("hashed");
        return em.persistAndFlush(user);
    }

    private Game saveGame(Long rawgId, String title) {
        Game game = new Game();
        game.setRawgId(rawgId);
        game.setTitle(title);
        return em.persistAndFlush(game);
    }

    // LibraryEntry sets addedAt itself when it is saved (@PrePersist), so entries saved by one test would all have
    // almost the same time. The time is therefore overwritten with plain SQL, which keeps the ordering tests exact.
    private LibraryEntry saveEntry(User user, Game game, Status status, Integer rating, LocalDateTime addedAt) {
        LibraryEntry entry = new LibraryEntry();
        entry.setUser(user);
        entry.setGame(game);
        entry.setStatus(status);
        entry.setRating(rating);
        em.persistAndFlush(entry);

        em.getEntityManager()
                .createNativeQuery("UPDATE library_entries SET added_at = ? WHERE id = ?")
                .setParameter(1, addedAt)
                .setParameter(2, entry.getId())
                .executeUpdate();
        return entry;
    }

    // Writes pending changes to the database and empties Hibernate's first-level cache,
    // so the next repository call has to read everything back with SQL
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private boolean isLoaded(Object entity) {
        PersistenceUnitUtil util = em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        return util.isLoaded(entity);
    }

    private static List<String> titles(List<LibraryEntry> entries) {
        return entries.stream().map(entry -> entry.getGame().getTitle()).toList();
    }
}
