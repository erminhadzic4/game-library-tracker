package com.erminhadzic.gamelibrarytracker.controller;

import com.erminhadzic.gamelibrarytracker.client.RawgClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final RawgClient rawgClient;

    public GameController(RawgClient rawgClient) {
        this.rawgClient = rawgClient;
    }

    // Passes RAWG's JSON through unchanged for now; mapping it to DTOs comes later
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(@RequestParam String q) {
        if (q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must not be blank");
        }
        return rawgClient.searchGames(q);
    }
}
