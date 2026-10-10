package ru.nsu.masolygin.http;

import java.io.IOException;
import java.net.URI;

public interface ResourceFetcher {
    Resource fetch(URI uri) throws IOException, InterruptedException;
}
