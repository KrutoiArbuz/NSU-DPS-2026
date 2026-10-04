package ru.nsu.masolygin.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.nsu.masolygin.crypto.KeyMaterial;

@Timeout(15)
class KeyServiceTest {
    @Test
    void testPendingRequestsShareOneGeneration() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        KeyMaterial expected = new KeyMaterial(new byte[]{1}, new byte[]{2}, new byte[]{3});
        try (KeyService service = new KeyService(name -> {
            calls.incrementAndGet();
            started.countDown();
            await(release);
            return expected;
        }, 1)) {
            try {
                CompletableFuture<KeyMaterial> first = service.request("alice");
                assertTrue(started.await(5, TimeUnit.SECONDS));
                CompletableFuture<KeyMaterial> second = service.request("alice");
                assertSame(first, second);
                assertFalse(second.isDone());
                release.countDown();
                assertSame(expected, first.get(5, TimeUnit.SECONDS));
                assertSame(expected, second.get(5, TimeUnit.SECONDS));
                assertSame(first, service.request("alice"));
                assertEquals(1, calls.get());
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void testFixedPoolQueuesExcessRequests() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try (KeyService service = new KeyService(name -> {
            int count = active.incrementAndGet();
            maximum.accumulateAndGet(count, Math::max);
            started.countDown();
            try {
                await(release);
                return new KeyMaterial(new byte[]{1}, new byte[]{2}, new byte[]{3});
            } finally {
                active.decrementAndGet();
            }
        }, 2)) {
            try {
                List<CompletableFuture<KeyMaterial>> requests = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    requests.add(service.request("client-" + i));
                }
                assertTrue(started.await(5, TimeUnit.SECONDS));
                assertEquals(2, active.get());
                assertTrue(requests.stream().noneMatch(CompletableFuture::isDone));
                release.countDown();
                CompletableFuture.allOf(requests.toArray(CompletableFuture<?>[]::new)).get(5, TimeUnit.SECONDS);
                assertEquals(2, maximum.get());
            } finally {
                release.countDown();
            }
        }
    }

    private void await(CountDownLatch latch) throws GeneralSecurityException {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new GeneralSecurityException("Test generator timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeneralSecurityException("Test generator interrupted", e);
        }
    }
}
