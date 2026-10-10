package ru.nsu.masolygin.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nsu.masolygin.crawler.Crawler;

@Timeout(15)
class HttpResourceFetcherTest {
    @Test
    void testCrawlsRealHttpGraphConcurrently() throws Exception {
        Map<String, String> pages = Map.of("/", "{\"message\":\"Я\",\"successors\":[\"/a\",\"/b\",\"/a#again\"]}", "/a", "{\"message\":\"same\",\"successors\":[\"/c\",\"/\"]}", "/b", "{\"message\":\"same\",\"successors\":[\"/c\"]}", "/c", "{\"message\":\"a\\\"b\",\"successors\":[]}");
        Map<String, AtomicInteger> visits = new ConcurrentHashMap<>();
        CountDownLatch siblings = new CountDownLatch(2);
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            try (exchange) {
                assertEquals("GET", exchange.getRequestMethod());
                String path = exchange.getRequestURI().getPath();
                visits.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
                if (path.equals("/a") || path.equals("/b")) {
                    siblings.countDown();
                    try {
                        assertTrue(siblings.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Handler interrupted", e);
                    }
                }
                respond(exchange, 200, "application/json; charset=UTF-8", pages.get(path));
            }
        }); HttpResourceFetcher fetcher = new HttpResourceFetcher(Duration.ofSeconds(10))) {
            assertEquals(List.of("a\"b", "same", "same", "Я"), new Crawler(fetcher, server.root(), 4).crawl());
            assertEquals(4, visits.size());
            for (AtomicInteger count : visits.values()) {
                assertEquals(1, count.get());
            }
        }
    }

    @Test
    void testRejectsHttpErrorAndWrongContentType() throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            try (exchange) {
                boolean failed = exchange.getRequestURI().getPath().equals("/failed");
                respond(exchange, failed ? 503 : 200, "text/plain", "unavailable");
            }
        }); HttpResourceFetcher fetcher = new HttpResourceFetcher(Duration.ofSeconds(5))) {
            IOException error = assertThrows(IOException.class, () -> fetcher.fetch(server.root().resolve("/failed")));
            assertTrue(error.getMessage().contains("503"));
            assertThrows(IOException.class, () -> fetcher.fetch(server.root()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{}", "{\"message\":1,\"successors\":[]}", "{\"message\":\"ok\",\"successors\":[1]}"})
    void testRejectsInvalidJson(String body) throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            try (exchange) {
                respond(exchange, 200, "application/json", body);
            }
        }); HttpResourceFetcher fetcher = new HttpResourceFetcher(Duration.ofSeconds(5))) {
            assertThrows(IOException.class, () -> fetcher.fetch(server.root()));
        }
    }

    @Test
    void testRequestTimeout() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            try (exchange) {
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }); HttpResourceFetcher fetcher = new HttpResourceFetcher(Duration.ofMillis(200))) {
            try {
                assertThrows(HttpTimeoutException.class, () -> fetcher.fetch(server.root()));
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void testReadsEmptyMessage() throws Exception {
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            try (exchange) {
                respond(exchange, 200, "APPLICATION/JSON", "{\"message\":\"\",\"successors\":[\"/next\"]}");
            }
        }); HttpResourceFetcher fetcher = new HttpResourceFetcher(Duration.ofSeconds(5))) {
            assertEquals(new Resource("", List.of("/next")), fetcher.fetch(server.root()));
        }
    }

    private void respond(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
