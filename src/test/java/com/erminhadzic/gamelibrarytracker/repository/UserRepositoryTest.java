package com.erminhadzic.gamelibrarytracker.repository;

import com.erminhadzic.gamelibrarytracker.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Repository slice test against H2: see LibraryEntryRepositoryTest for how @DataJpaTest works
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByUsername_returnsTheUser() {
        em.persistAndFlush(user("alice", "alice@test.local"));
        em.clear();

        assertThat(userRepository.findByUsername("alice"))
                .hasValueSatisfying(found -> assertThat(found.getEmail()).isEqualTo("alice@test.local"));
    }

    @Test
    void findByUsername_unknownName_isEmpty() {
        em.persistAndFlush(user("alice", "alice@test.local"));
        em.clear();

        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void existsByUsernameAndExistsByEmail_matchSavedUsersOnly() {
        em.persistAndFlush(user("alice", "alice@test.local"));
        em.clear();

        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
        assertThat(userRepository.existsByEmail("alice@test.local")).isTrue();
        assertThat(userRepository.existsByEmail("bob@test.local")).isFalse();
    }

    // The user is given a creation time when it is first saved (@PrePersist)
    @Test
    void save_setsCreatedAt() {
        User saved = userRepository.saveAndFlush(user("alice", "alice@test.local"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void duplicateUsername_throwsDataIntegrityViolation() {
        em.persistAndFlush(user("alice", "alice@test.local"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("alice", "other@test.local")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateEmail_throwsDataIntegrityViolation() {
        em.persistAndFlush(user("alice", "alice@test.local"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("bob", "alice@test.local")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User user(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashed");
        return user;
    }
}
