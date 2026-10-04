package com.erminhadzic.gamelibrarytracker.controller;

import com.erminhadzic.gamelibrarytracker.client.RawgClient;
import com.erminhadzic.gamelibrarytracker.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Games", description = "Game data from the RAWG API. The responses are RAWG's JSON, passed through unchanged.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class GameController {

    private final RawgClient rawgClient;

    public GameController(RawgClient rawgClient) {
        this.rawgClient = rawgClient;
    }

    // Passes RAWG's JSON through unchanged for now; mapping it to DTOs comes later
    @Operation(summary = "Search games by name")
    @ApiResponse(responseCode = "200", description = "RAWG's search result (the games are in its \"results\" array)")
    @ApiResponse(responseCode = "400", description = "q is missing or blank",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(@Parameter(description = "Text to search for", example = "witcher") @RequestParam String q) {
        if (q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must not be blank");
        }
        return rawgClient.searchGames(q);
    }

    // Ready-made lists for the Search page before the user types anything.
    // type=popular: popular games of the last 12 months. type=top: the best rated games of all time.
    @Operation(summary = "Get a ready-made list: popular games of the last 12 months or the best rated of all time")
    @ApiResponse(responseCode = "200", description = "RAWG's list of 12 games (in its \"results\" array)")
    @ApiResponse(responseCode = "400", description = "type is missing or is not popular / top",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(value = "/discover", produces = MediaType.APPLICATION_JSON_VALUE)
    public String discover(@Parameter(description = "Which list to return",
            schema = @Schema(allowableValues = {"popular", "top"})) @RequestParam String type) {
        return switch (type) {
            case "popular" -> rawgClient.getPopularGames();
            case "top" -> rawgClient.getTopRatedGames();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be popular or top");
        };
    }
}
