package ru.nsu.masolygin.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class ResourcePaths {
    private final URI root;

    public ResourcePaths(URI server) {
        if (server.getHost() == null || server.getUserInfo() != null || !("http".equalsIgnoreCase(server.getScheme()) || "https".equalsIgnoreCase(server.getScheme()))) {
            throw new IllegalArgumentException("Server must be an HTTP(S) URL with a host");
        }
        try {
            root = new URI(server.getScheme().toLowerCase(Locale.ROOT), null, server.getHost().toLowerCase(Locale.ROOT), server.getPort(), "/", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid server URL", e);
        }
    }

    public URI root() {
        return root;
    }

    public URI resolve(String path) {
        URI target = root.resolve(path).normalize();
        if (!root.getScheme().equalsIgnoreCase(target.getScheme()) || !root.getHost().equalsIgnoreCase(target.getHost()) || port(root) != port(target) || target.getUserInfo() != null) {
            throw new IllegalArgumentException("Successor leaves configured server: " + path);
        }
        String pathPart = target.getRawPath();
        String query = target.getRawQuery();
        String suffix = (pathPart == null || pathPart.isEmpty() ? "/" : pathPart) + (query == null ? "" : "?" + query);
        return URI.create(root.getScheme() + "://" + root.getRawAuthority() + suffix);
    }

    private int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80);
    }
}
