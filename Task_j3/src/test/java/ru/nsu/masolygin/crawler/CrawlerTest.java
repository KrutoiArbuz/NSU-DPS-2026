package ru.nsu.masolygin.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.nsu.masolygin.http.ResourceFetcher;
import ru.nsu.masolygin.http.Resource;

@Timeout(15)
class CrawlerTest {
    @Test
    void testVisitsUniqueUrlsOnVirtualThreadsAndKeepsDuplicateMessages() throws Exception {
        Map<String, Resource> graph = Map.of("/", new Resource("z", List.of("/a", "/b", "/a#same")), "/a", new Resource("same", List.of("/c", "/")), "/b", new Resource("same", List.of("/c")), "/c", new Resource("a", List.of()));
        Map<String, AtomicInteger> visits = new ConcurrentHashMap<>();
        ResourceFetcher fetcher = uri -> {
            assertTrue(Thread.currentThread().isVirtual());
            visits.computeIfAbsent(uri.getPath(), ignored -> new AtomicInteger()).incrementAndGet();
            return graph.get(uri.getPath());
        };
        assertEquals(List.of("a", "same", "same", "z"), new Crawler(fetcher, URI.create("http://localhost"), 4).crawl());
        assertEquals(4, visits.size());
        for (AtomicInteger count : visits.values()) {
            assertEquals(1, count.get());
        }
    }

    @Test
    void testParallelLimit() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        ResourceFetcher fetcher = uri -> {
            if (uri.getPath().equals("/")) {
                return new Resource("root", List.of("/a", "/b", "/c"));
            }
            int current = active.incrementAndGet();
            maximum.accumulateAndGet(current, Math::max);
            entered.countDown();
            try {
                release.await();
                return new Resource(uri.getPath(), List.of());
            } finally {
                active.decrementAndGet();
            }
        };
        try (var coordinator = Executors.newSingleThreadExecutor()) {
            var result = coordinator.submit(() -> new Crawler(fetcher, URI.create("http://localhost"), 2).crawl());
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(2, active.get());
            } finally {
                release.countDown();
            }
            assertEquals(List.of("/a", "/b", "/c", "root"), result.get(5, TimeUnit.SECONDS));
            assertEquals(2, maximum.get());
        }
    }

    @Test
    void testFastBranchExpandsBeforeSlowNeighbourFinishes() throws Exception {
        CountDownLatch descendant = new CountDownLatch(1);
        ResourceFetcher fetcher = uri -> {
            return switch (uri.getPath()) {
                case "/" -> new Resource("root", List.of("/slow", "/fast"));
                case "/fast" -> new Resource("fast", List.of("/child"));
                case "/child" -> {
                    descendant.countDown();
                    yield new Resource("child", List.of());
                }
                case "/slow" -> {
                    if (!descendant.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Ready branch was not expanded");
                    }
                    yield new Resource("slow", List.of());
                }
                default -> throw new IOException("Unexpected path");
            };
        };
        assertEquals(List.of("child", "fast", "root", "slow"), new Crawler(fetcher, URI.create("http://localhost"), 2).crawl());
    }

    @Test
    void testFailureInterruptsOtherRequests() {
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        ResourceFetcher fetcher = uri -> {
            if (uri.getPath().equals("/")) {
                return new Resource("root", List.of("/blocked", "/failed"));
            }
            if (uri.getPath().equals("/failed")) {
                if (!waiting.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Neighbour did not start");
                }
                throw new IOException("Expected failure");
            }
            waiting.countDown();
            try {
                new CountDownLatch(1).await();
                throw new IOException("Unexpected completion");
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
            }
        };
        IOException error = assertThrows(IOException.class, () -> new Crawler(fetcher, URI.create("http://localhost"), 2).crawl());
        assertEquals("Expected failure", error.getCause().getMessage());
        assertEquals(0, interrupted.getCount());
    }
}
