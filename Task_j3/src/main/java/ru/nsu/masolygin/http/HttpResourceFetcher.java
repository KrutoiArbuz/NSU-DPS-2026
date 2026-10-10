package ru.nsu.masolygin.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HttpResourceFetcher implements ResourceFetcher, AutoCloseable {
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Duration timeout;

    public HttpResourceFetcher(Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("HTTP timeout must be positive");
        }
        this.timeout = timeout;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public Resource fetch(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " at " + uri);
        }
        String type = response.headers().firstValue("Content-Type").orElse("").split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!type.equals("application/json")) {
            throw new IOException("Expected application/json at " + uri);
        }
        JsonNode root = mapper.readTree(response.body());
        if (root == null || !root.isObject() || !root.path("message").isTextual() || !root.path("successors").isArray()) {
            throw new IOException("Invalid resource JSON at " + uri);
        }
        List<String> paths = new ArrayList<>();
        for (JsonNode successor : root.path("successors")) {
            if (!successor.isTextual()) {
                throw new IOException("Successor must be a string at " + uri);
            }
            paths.add(successor.textValue());
        }
        return new Resource(root.path("message").textValue(), paths);
    }

    @Override
    public void close() {
        client.close();
    }
}
