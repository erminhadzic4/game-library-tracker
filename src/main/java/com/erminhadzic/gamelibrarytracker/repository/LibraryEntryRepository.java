package com.erminhadzic.gamelibrarytracker.repository;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    // @EntityGraph fetches the game in the same query, avoiding one extra SELECT per entry (N+1)
    // Newest first, so the order is stable and recently added games come first
    @EntityGraph(attributePaths = "game")
    List<LibraryEntry> findByUserOrderByAddedAtDesc(User user);

    @EntityGraph(attributePaths = "game")
    List<LibraryEntry> findByUserAndStatusOrderByAddedAtDesc(User user, LibraryEntry.Status status);

    Optional<LibraryEntry> findByUserAndGame_Id(User user, Long gameId);

    // Looks up an entry only if it belongs to the given user, so ownership is enforced in the query itself
    Optional<LibraryEntry> findByIdAndUser(Long id, User user);

    long countByUserAndStatus(User user, LibraryEntry.Status status);

    // AVG ignores NULL ratings and returns NULL when no entry is rated
    @Query("SELECT AVG(e.rating) FROM LibraryEntry e WHERE e.user = :user")
    Double findAverageRatingByUser(@Param("user") User user);
}
