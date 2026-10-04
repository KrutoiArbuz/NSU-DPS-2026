package ru.nsu.masolygin.server;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.nsu.masolygin.crypto.KeyGenerator;
import ru.nsu.masolygin.crypto.KeyMaterial;

class KeyService implements AutoCloseable {
    private final KeyGenerator generator;
    private final ExecutorService workers;
    private final Map<String, CompletableFuture<KeyMaterial>> results = new HashMap<>();

    KeyService(KeyGenerator generator, int workerCount) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }
        this.generator = generator;
        this.workers = Executors.newFixedThreadPool(workerCount);
    }

    synchronized CompletableFuture<KeyMaterial> request(String name) {
        return results.computeIfAbsent(name, ignored -> CompletableFuture.supplyAsync(() -> {
            try {
                return generator.generate(name);
            } catch (GeneralSecurityException | IOException e) {
                throw new CompletionException(e);
            }
        }, workers));
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
