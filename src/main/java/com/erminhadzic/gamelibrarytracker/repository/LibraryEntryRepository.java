package com.erminhadzic.gamelibrarytracker.repository;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    // @EntityGraph fetches the game in the same query, avoiding one extra SELECT per entry (N+1)
    @EntityGraph(attributePaths = "game")
    List<LibraryEntry> findByUser(User user);

    @EntityGraph(attributePaths = "game")
    List<LibraryEntry> findByUserAndStatus(User user, LibraryEntry.Status status);

    Optional<LibraryEntry> findByUserAndGame_Id(User user, Long gameId);

    // Looks up an entry only if it belongs to the given user, so ownership is enforced in the query itself
    Optional<LibraryEntry> findByIdAndUser(Long id, User user);
}
