package com.erminhadzic.gamelibrarytracker.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class RawgClient {

    private static final Logger log = LoggerFactory.getLogger(RawgClient.class);

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
    // Not cached: every query is different, so a cache would rarely be hit.
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
    // @Cacheable: the returned JSON is kept in the "discover" cache under the key "popular". While it is there,
    // Spring returns it without running this method. The method has no parameters, so the key is given by hand;
    // without it, both discover methods would share one key and return each other's list.
    // If RAWG fails, the exception passes through and nothing is stored.
    @Cacheable(cacheNames = "discover", key = "'popular'")
    public String getPopularGames() {
        // Only reached on a cache miss, so this line shows in the console when RAWG is really called
        log.info("Cache miss: fetching the popular games list from RAWG");
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
    // Cached like the popular list, under its own key.
    @Cacheable(cacheNames = "discover", key = "'top'")
    public String getTopRatedGames() {
        log.info("Cache miss: fetching the top rated games list from RAWG");
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
