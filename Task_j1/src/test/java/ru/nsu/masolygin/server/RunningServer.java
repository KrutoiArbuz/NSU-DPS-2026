package ru.nsu.masolygin.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ru.nsu.masolygin.crypto.KeyGenerator;

public class RunningServer implements AutoCloseable {
    private final KeyService service;
    private final KeyServer server;
    private final Thread thread;
    private final CompletableFuture<Void> stopped = new CompletableFuture<>();
    private final int port;

    public RunningServer(KeyGenerator generator, int workers) throws IOException {
        service = new KeyService(generator, workers);
        server = new KeyServer(new InetSocketAddress("127.0.0.1", 0), service);
        port = server.port();
        thread = new Thread(() -> {
            try {
                server.run();
                stopped.complete(null);
            } catch (IOException | RuntimeException e) {
                stopped.completeExceptionally(e);
            }
        }, "test-selector");
        thread.start();
    }

    public int port() {
        return port;
    }

    @Override
    public void close() throws IOException {
        server.close();
        service.close();
        try {
            stopped.get(5, TimeUnit.SECONDS);
            thread.join(5_000);
            if (thread.isAlive()) {
                throw new IOException("Server thread did not stop");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping server", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException("Server failed or did not stop", e);
        }
    }
}
