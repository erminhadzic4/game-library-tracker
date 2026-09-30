package com.erminhadzic.gamelibrarytracker.service;

import com.erminhadzic.gamelibrarytracker.dto.AuthResponse;
import com.erminhadzic.gamelibrarytracker.dto.LoginRequest;
import com.erminhadzic.gamelibrarytracker.dto.RegisterRequest;
import com.erminhadzic.gamelibrarytracker.model.User;
import com.erminhadzic.gamelibrarytracker.repository.UserRepository;
import com.erminhadzic.gamelibrarytracker.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Pure unit tests: the repository, encoder and JWT service are Mockito mocks, so no Spring context or database
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // --- register ---

    @Test
    void register_savesUserWithHashedPasswordAndReturnsToken() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.local")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(jwtService.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.register(new RegisterRequest("alice", "alice@test.local", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getEmail()).isEqualTo("alice@test.local");
        // The raw password must never be stored
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void register_duplicateUsername_returns409() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertStatus(() -> authService.register(new RegisterRequest("alice", "alice@test.local", "secret123")),
                HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_returns409() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.local")).thenReturn(true);

        assertStatus(() -> authService.register(new RegisterRequest("alice", "alice@test.local", "secret123")),
                HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_missingFields_returns400() {
        assertStatus(() -> authService.register(new RegisterRequest(null, "alice@test.local", "secret123")),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> authService.register(new RegisterRequest("alice", " ", "secret123")),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> authService.register(new RegisterRequest("alice", "alice@test.local", "")),
                HttpStatus.BAD_REQUEST);
        // Validation fails before the database is touched
        verifyNoInteractions(userRepository);
    }

    // --- login ---

    @Test
    void login_correctPassword_returnsToken() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", "hashed")));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("alice", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_returns401() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", "hashed")));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertStatus(() -> authService.login(new LoginRequest("alice", "wrong")), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_unknownUsername_returns401() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertStatus(() -> authService.login(new LoginRequest("nobody", "secret123")), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtService);
    }

    private static User user(String username, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordHash);
        return user;
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
