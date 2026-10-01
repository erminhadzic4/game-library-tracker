package com.erminhadzic.gamelibrarytracker.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class RawgClient {

    private static final int DISCOVER_PAGE_SIZE = 12;

    private final RestClient restClient;
    private final String apiKey;

    public RawgClient(RawgProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
        this.apiKey = properties.apiKey();
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

    // The 12 games added to the most RAWG user libraries, out of those released in the last 12 months.
    // Calls GET {base-url}/games?dates={a year ago},{today}&ordering=-added&page_size=12&key={api-key}.
    public String getPopularGames() {
        LocalDate today = LocalDate.now();
        // RAWG takes a date range as "from,to" in ISO format, which is what LocalDate.toString() gives
        String lastTwelveMonths = today.minusMonths(12) + "," + today;
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("dates", lastTwelveMonths)
                        .queryParam("ordering", "-added")
                        .queryParam("page_size", DISCOVER_PAGE_SIZE)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
    }

    // The 12 games with the highest Metacritic score of all time.
    // Calls GET {base-url}/games?ordering=-metacritic&page_size=12&key={api-key}.
    public String getTopRatedGames() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("ordering", "-metacritic")
                        .queryParam("page_size", DISCOVER_PAGE_SIZE)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
