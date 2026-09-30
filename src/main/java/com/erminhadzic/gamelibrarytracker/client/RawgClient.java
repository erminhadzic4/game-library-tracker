package com.erminhadzic.gamelibrarytracker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RawgClient {

    private final RestClient restClient;
    private final String apiKey;

    public RawgClient(@Value("${rawg.base-url}") String baseUrl,
                      @Value("${rawg.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    // Calls GET {base-url}/games?search={query}&search_precise=true&key={api-key} and returns the JSON body as-is.
    // search_precise drops loose partial matches (e.g. "witcher" goes from ~2600 hits to ~45, with the real games on top).
    public String searchGames(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("search", query)
                        .queryParam("search_precise", true)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
