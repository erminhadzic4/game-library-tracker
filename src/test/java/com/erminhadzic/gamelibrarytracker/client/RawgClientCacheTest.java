package com.erminhadzic.gamelibrarytracker.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Proves the cache by counting real HTTP requests. A small web server from the JDK plays the part of RAWG:
// rawg.base-url points at it, so RawgClient is used unchanged. @Cacheable only works on the Spring bean
// (Spring wraps it in a proxy), so the test needs the application context and not "new RawgClient(...)".
@SpringBootTest
@ActiveProfiles("test")
class RawgClientCacheTest {

    // The query string of every request the fake RAWG received
    private static final List<String> requests = new CopyOnWriteArrayList<>();
    // When true, the fake RAWG answers the next request with a 500
    private static final AtomicBoolean failNextRequest = new AtomicBoolean(false);

    private static final HttpServer fakeRawg = startFakeRawg();

    @Autowired
    private RawgClient rawgClient;

    @Autowired
    private CacheManager cacheManager;

    @DynamicPropertySource
    static void pointRawgClientAtTheFakeServer(DynamicPropertyRegistry registry) {
        registry.add("rawg.base-url", () -> "http://localhost:" + fakeRawg.getAddress().getPort());
    }

    @AfterAll
    static void stopFakeRawg() {
        fakeRawg.stop(0);
    }

    // The cache belongs to the application context, which all tests in this class share
    @BeforeEach
    void emptyCacheAndRequestLog() {
        cacheManager.getCache("discover").clear();
        requests.clear();
        failNextRequest.set(false);
    }

    @Test
    void popularGames_secondCallIsServedFromTheCache() {
        String first = rawgClient.getPopularGames();
        String second = rawgClient.getPopularGames();

        assertThat(first).isEqualTo("{\"results\":[\"popular\"]}");
        assertThat(second).isEqualTo(first);
        assertThat(requests).hasSize(1);
    }

    @Test
    void topRatedGames_secondCallIsServedFromTheCache() {
        rawgClient.getTopRatedGames();
        rawgClient.getTopRatedGames();

        assertThat(requests).hasSize(1);
    }

    // Both methods use the same cache; without their own keys one list would be returned for the other
    @Test
    void popularAndTopRated_areCachedSeparately() {
        String popular = rawgClient.getPopularGames();
        String top = rawgClient.getTopRatedGames();

        assertThat(popular).isEqualTo("{\"results\":[\"popular\"]}");
        assertThat(top).isEqualTo("{\"results\":[\"top\"]}");
        assertThat(requests).hasSize(2);
    }

    @Test
    void failedCall_isNotCached() {
        failNextRequest.set(true);
        assertThatThrownBy(() -> rawgClient.getPopularGames()).isInstanceOf(HttpServerErrorException.class);

        // The error was not remembered: the next call goes to RAWG again and gets the list
        assertThat(rawgClient.getPopularGames()).isEqualTo("{\"results\":[\"popular\"]}");
        assertThat(requests).hasSize(2);
    }

    @Test
    void search_isNotCached() {
        rawgClient.searchGames("witcher");
        rawgClient.searchGames("witcher");

        assertThat(requests).hasSize(2);
    }

    // Answers GET /games the way RAWG would, with a tiny body that tells the three kinds of request apart
    private static HttpServer startFakeRawg() {
        try {
            // Port 0: the operating system picks a free port
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/games", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                requests.add(query);

                int status = failNextRequest.getAndSet(false) ? 500 : 200;
                String kind = query.contains("search=") ? "search"
                        : query.contains("ordering=-metacritic") ? "top" : "popular";
                byte[] body = ("{\"results\":[\"" + kind + "\"]}").getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the fake RAWG server", e);
        }
    }
}
