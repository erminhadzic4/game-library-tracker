package com.erminhadzic.gamelibrarytracker.controller;

import com.erminhadzic.gamelibrarytracker.client.RawgClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

// Pure unit tests: the RAWG client is a Mockito mock, so no Spring context and no call to the real API
@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private RawgClient rawgClient;

    @InjectMocks
    private GameController gameController;

    @Test
    void discoverPopularReturnsThePopularGames() {
        when(rawgClient.getPopularGames()).thenReturn("{\"results\": []}");

        String body = gameController.discover("popular");

        assertThat(body).isEqualTo("{\"results\": []}");
        verify(rawgClient).getPopularGames();
        verifyNoMoreInteractions(rawgClient);
    }

    @Test
    void discoverTopReturnsTheTopRatedGames() {
        when(rawgClient.getTopRatedGames()).thenReturn("{\"results\": []}");

        String body = gameController.discover("top");

        assertThat(body).isEqualTo("{\"results\": []}");
        verify(rawgClient).getTopRatedGames();
        verifyNoMoreInteractions(rawgClient);
    }

    @Test
    void discoverWithUnknownTypeReturns400WithoutCallingRawg() {
        assertThatThrownBy(() -> gameController.discover("newest"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(rawgClient);
    }

    @Test
    void discoverWithBlankTypeReturns400() {
        assertThatThrownBy(() -> gameController.discover(""))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(rawgClient);
    }

    // The type is case-sensitive: only the exact lower-case values are accepted
    @Test
    void discoverWithUpperCaseTypeReturns400() {
        assertThatThrownBy(() -> gameController.discover("POPULAR"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(rawgClient);
    }

    @Test
    void searchWithBlankQueryStillReturns400() {
        assertThatThrownBy(() -> gameController.search("  "))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(rawgClient);
    }
}
