package com.erminhadzic.gamelibrarytracker.repository;

import com.erminhadzic.gamelibrarytracker.model.LibraryEntry;
import com.erminhadzic.gamelibrarytracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    List<LibraryEntry> findByUser(User user);
    List<LibraryEntry> findByUserAndStatus(User user, LibraryEntry.Status status);
    Optional<LibraryEntry> findByUserAndGame_Id(User user, Long gameId);
}