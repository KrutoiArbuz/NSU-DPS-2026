package ru.nsu.masolygin.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TestHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();

    TestHttpServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(handlers);
        server.createContext("/", handler);
        server.start();
    }

    URI root() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @Override
    public void close() {
        server.stop(0);
        handlers.shutdownNow();
        handlers.close();
    }
}
