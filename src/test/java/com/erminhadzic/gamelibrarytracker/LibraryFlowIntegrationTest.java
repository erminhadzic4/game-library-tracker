package com.erminhadzic.gamelibrarytracker;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Library endpoints through the real filter chain, controllers, services and JPA (H2), with Open Session in View off.
// Unlike the Mockito unit tests, this would fail with a LazyInitializationException if a response were built from a
// lazy association outside a transaction. RAWG is never called on this path, so RawgClient isn't mocked.
// The tests share one database, so each one registers its own users.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    // Boot registers this interceptor only when spring.jpa.open-in-view is true, so its absence shows the setting is applied
    @Test
    void openSessionInViewIsOff() {
        assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
    }

    @Test
    void registerThenAddThenListThenUpdateThenStats() throws Exception {
        String auth = registerAndGetAuthHeader("libflow");

        String addBody = mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawgId": 3328, "title": "The Witcher 3: Wild Hunt",
                                 "coverImageUrl": "https://example.test/witcher3.jpg",
                                 "releaseDate": "2015-05-18", "status": "BACKLOG"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawgId").value(3328))
                .andExpect(jsonPath("$.title").value("The Witcher 3: Wild Hunt"))
                .andExpect(jsonPath("$.status").value("BACKLOG"))
                .andReturn().getResponse().getContentAsString();
        Number entryId = JsonPath.read(addBody, "$.id");

        mockMvc.perform(get("/api/library").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("The Witcher 3: Wild Hunt"))
                .andExpect(jsonPath("$[0].releaseDate").value("2015-05-18"));

        // The update loads the entry without @EntityGraph, so its game is a lazy proxy that the response has to read
        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": 9, "notes": "Great so far"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYING"))
                .andExpect(jsonPath("$.rating").value(9))
                .andExpect(jsonPath("$.notes").value("Great so far"))
                .andExpect(jsonPath("$.title").value("The Witcher 3: Wild Hunt"))
                .andExpect(jsonPath("$.coverImageUrl").value("https://example.test/witcher3.jpg"));

        // The change was saved, and the status filter finds the entry under its new status only
        mockMvc.perform(get("/api/library").param("status", "PLAYING").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rating").value(9))
                .andExpect(jsonPath("$[0].title").value("The Witcher 3: Wild Hunt"));
        mockMvc.perform(get("/api/library").param("status", "BACKLOG").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/library/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.playing").value(1))
                .andExpect(jsonPath("$.backlog").value(0))
                .andExpect(jsonPath("$.completed").value(0))
                .andExpect(jsonPath("$.averageRating").value(9.0));
    }

    @Test
    void updateCanClearRatingAndNotes() throws Exception {
        String auth = registerAndGetAuthHeader("clearflow");
        Number entryId = addGame(auth, 4200, "Portal 2");

        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "COMPLETED", "rating": 10, "notes": "Still the best"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(10))
                .andExpect(jsonPath("$.notes").value("Still the best"));
        mockMvc.perform(get("/api/library/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(10.0));

        // A null rating clears it; whitespace-only notes are stored as null by the server
        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "COMPLETED", "rating": null, "notes": "   "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.rating").value(nullValue()))
                .andExpect(jsonPath("$.notes").value(nullValue()));

        // Read back from the database: both are gone, and the average no longer counts the entry
        mockMvc.perform(get("/api/library").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rating").value(nullValue()))
                .andExpect(jsonPath("$[0].notes").value(nullValue()));
        mockMvc.perform(get("/api/library/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.averageRating").value(nullValue()));
    }

    @Test
    void updateWithoutStatusReturns400() throws Exception {
        String auth = registerAndGetAuthHeader("nostatus");
        Number entryId = addGame(auth, 4291, "Counter-Strike: Global Offensive");

        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 7}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingAnotherUsersEntryReturns404AndChangesNothing() throws Exception {
        String ownerAuth = registerAndGetAuthHeader("entryowner");
        String otherAuth = registerAndGetAuthHeader("entryother");
        Number entryId = addGame(ownerAuth, 5286, "Tomb Raider");

        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "COMPLETED", "rating": 1, "notes": "not mine"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/library").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("BACKLOG"))
                .andExpect(jsonPath("$[0].rating").value(nullValue()))
                .andExpect(jsonPath("$[0].notes").value(nullValue()));
    }

    // The update used to be a PATCH (partial, null = unchanged). It was replaced by PUT on purpose, so PATCH is rejected.
    @Test
    void oldPatchUpdateReturns405() throws Exception {
        String auth = registerAndGetAuthHeader("patchgone");
        Number entryId = addGame(auth, 12020, "Left 4 Dead 2");

        mockMvc.perform(patch("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": 8, "notes": null}
                                """))
                .andExpect(status().isMethodNotAllowed());
    }

    private String registerAndGetAuthHeader(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "email": "%s@test.local", "password": "secret123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(body, "$.token");
    }

    // Adds a game with status BACKLOG and returns the new entry's id
    private Number addGame(String auth, int rawgId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawgId": %d, "title": "%s", "status": "BACKLOG"}
                                """.formatted(rawgId, title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
