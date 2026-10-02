package com.erminhadzic.gamelibrarytracker.repository;

import com.erminhadzic.gamelibrarytracker.model.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Repository slice test against H2: see LibraryEntryRepositoryTest for how @DataJpaTest works
@DataJpaTest
@ActiveProfiles("test")
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByRawgId_returnsTheGameWithAllItsFields() {
        Game game = game(3328L, "The Witcher 3");
        game.setCoverImageUrl("https://example.test/witcher3.jpg");
        game.setReleaseDate(LocalDate.of(2015, 5, 18));
        em.persistAndFlush(game);
        em.clear();

        assertThat(gameRepository.findByRawgId(3328L)).hasValueSatisfying(found -> {
            assertThat(found.getTitle()).isEqualTo("The Witcher 3");
            assertThat(found.getCoverImageUrl()).isEqualTo("https://example.test/witcher3.jpg");
            assertThat(found.getReleaseDate()).isEqualTo(LocalDate.of(2015, 5, 18));
        });
    }

    @Test
    void findByRawgId_unknownId_isEmpty() {
        em.persistAndFlush(game(3328L, "The Witcher 3"));
        em.clear();

        assertThat(gameRepository.findByRawgId(1L)).isEmpty();
    }

    // Games are shared by all users, so one RAWG game must exist only once
    @Test
    void duplicateRawgId_throwsDataIntegrityViolation() {
        em.persistAndFlush(game(3328L, "The Witcher 3"));

        assertThatThrownBy(() -> gameRepository.saveAndFlush(game(3328L, "The Witcher 3 (again)")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Game game(Long rawgId, String title) {
        Game game = new Game();
        game.setRawgId(rawgId);
        game.setTitle(title);
        return game;
    }
}
