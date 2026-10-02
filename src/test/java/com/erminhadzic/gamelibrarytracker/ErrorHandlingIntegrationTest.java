package com.erminhadzic.gamelibrarytracker;

import com.erminhadzic.gamelibrarytracker.client.RawgClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every kind of error comes back as a ProblemDetail JSON body (application/problem+json) with the right status.
// Validation is tested here and not in the service unit tests, because @Valid rejects the request before the
// service is called. RawgClient is a mock, so the 500 case can be triggered without calling RAWG.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingIntegrationTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RawgClient rawgClient;

    // --- validation: 400 with an "errors" list that names each field ---

    @Test
    void registerWithInvalidFieldsReturns400NamingEachField() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "ab", "email": "not-an-email", "password": "short"}
                        """))
                .andDo(printBody("validation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password", "username")))
                .andExpect(jsonPath("$.errors[*].message", containsInAnyOrder(
                        "Email must be a valid email address",
                        "Password must be 8 to 72 characters long",
                        "Username must be 3 to 50 characters long")));
    }

    @Test
    void registerWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password", "username")))
                .andExpect(jsonPath("$.errors[*].message", containsInAnyOrder(
                        "Email is required", "Password is required", "Username is required")));
    }

    @Test
    void registerWithTooLongFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "email": "longuser@test.local", "password": "%s"}
                        """.formatted("u".repeat(51), "p".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("password", "username")));
    }

    @Test
    void loginWithBlankFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": " ", "password": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("password", "username")));
    }

    @Test
    void addWithMissingFieldsReturns400() throws Exception {
        String auth = registerAndGetAuthHeader("erradd");

        mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("rawgId", "status", "title")));
    }

    @Test
    void addWithTooLongTitleOrCoverUrlReturns400() throws Exception {
        String auth = registerAndGetAuthHeader("errlong");

        mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawgId": 1, "title": "%s", "coverImageUrl": "%s", "status": "BACKLOG"}
                                """.formatted("t".repeat(256), "c".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("coverImageUrl", "title")));
    }

    @Test
    void updateWithInvalidFieldsReturns400NamingEachField() throws Exception {
        String auth = registerAndGetAuthHeader("errupdate");
        Number entryId = addGame(auth, 801, "Hades");

        // No status, a rating above 10 and notes over the 2000 character limit
        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 11, "notes": "%s"}
                                """.formatted("n".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("notes", "rating", "status")))
                .andExpect(jsonPath("$.errors[*].message", hasItems(
                        "status is required", "rating must be between 1 and 10")));

        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("rating")));

        // The boundaries are valid: rating 1 and 10, notes of exactly 2000 characters
        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": 1, "notes": "%s"}
                                """.formatted("n".repeat(2000))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/library/" + entryId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": 10, "notes": null}
                                """))
                .andExpect(status().isOk());
    }

    // --- requests Spring MVC rejects itself ---

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\": "))
                .andDo(printBody("malformed JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void unknownStatusValueReturns400() throws Exception {
        String auth = registerAndGetAuthHeader("errenum");

        mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawgId": 2, "title": "Celeste", "status": "FINISHED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void missingQueryParameterReturns400() throws Exception {
        String auth = registerAndGetAuthHeader("errparam");

        mockMvc.perform(get("/api/games/search").header("Authorization", auth))
                .andDo(printBody("missing parameter"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(rawgClient);
    }

    @Test
    void nonNumericEntryIdReturns400() throws Exception {
        String auth = registerAndGetAuthHeader("errid");

        mockMvc.perform(put("/api/library/abc").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": null, "notes": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        String auth = registerAndGetAuthHeader("errmethod");

        mockMvc.perform(patch("/api/library/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andDo(printBody("wrong method"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        String auth = registerAndGetAuthHeader("errroute");

        mockMvc.perform(get("/api/does-not-exist").header("Authorization", auth))
                .andDo(printBody("unknown route"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No endpoint at this path"));
    }

    // --- errors thrown by the services (ResponseStatusException): status and reason are kept ---

    @Test
    void duplicateAddReturns409() throws Exception {
        String auth = registerAndGetAuthHeader("errdup");
        addGame(auth, 802, "Hollow Knight");

        mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawgId": 802, "title": "Hollow Knight", "status": "PLAYING"}
                                """))
                .andDo(printBody("conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("This game is already in your library"));
    }

    @Test
    void duplicateUsernameOrEmailReturns409() throws Exception {
        registerAndGetAuthHeader("errtaken");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "errtaken", "email": "other@test.local", "password": "secret123"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Username is already taken"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "errtaken2", "email": "errtaken@test.local", "password": "secret123"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already registered"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        registerAndGetAuthHeader("errlogin");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "errlogin", "password": "not-the-password"}
                        """))
                .andDo(printBody("wrong password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void entryThatDoesNotExistReturns404() throws Exception {
        String auth = registerAndGetAuthHeader("errmissing");

        mockMvc.perform(put("/api/library/999999").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PLAYING", "rating": null, "notes": null}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Library entry not found"));
    }

    // --- 401 from the security filter chain (ProblemDetailAuthenticationEntryPoint) ---

    @Test
    void missingTokenReturns401WithProblemBody() throws Exception {
        mockMvc.perform(get("/api/library"))
                .andDo(printBody("no token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required: send a valid Bearer token"));
    }

    @Test
    void invalidTokenReturns401WithProblemBody() throws Exception {
        mockMvc.perform(get("/api/library").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- catch-all ---

    @Test
    void unexpectedExceptionReturns500WithoutInternals() throws Exception {
        String auth = registerAndGetAuthHeader("errboom");
        when(rawgClient.searchGames("zelda"))
                .thenThrow(new IllegalStateException("SELECT secret FROM internals: connection refused"));

        String body = mockMvc.perform(get("/api/games/search").param("q", "zelda").header("Authorization", auth))
                .andDo(printBody("unexpected error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andReturn().getResponse().getContentAsString();

        // Neither the message, the exception class nor a stack trace reaches the client
        assertThat(body).doesNotContain("SELECT", "secret", "IllegalStateException", "connection refused", "at com.");
    }

    // Prints the response body, so the shape of each kind of error can be read in the test output
    private static ResultHandler printBody(String label) {
        return result -> System.out.println("[error body: " + label + "] " + result.getResponse().getStatus()
                + " " + result.getResponse().getContentAsString());
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
