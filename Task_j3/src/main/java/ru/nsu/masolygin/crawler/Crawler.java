package ru.nsu.masolygin.crawler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.nsu.masolygin.http.ResourceFetcher;
import ru.nsu.masolygin.http.Resource;

public class Crawler {
    private final ResourceFetcher fetcher;
    private final ResourcePaths paths;
    private final int parallelism;

    public Crawler(ResourceFetcher fetcher, URI server, int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("Parallelism must be positive");
        }
        this.fetcher = fetcher;
        paths = new ResourcePaths(server);
        this.parallelism = parallelism;
    }

    public List<String> crawl() throws IOException, InterruptedException {
        Set<URI> visited = new HashSet<>();
        Queue<URI> pending = new ArrayDeque<>();
        List<String> messages = new ArrayList<>();
        visited.add(paths.root());
        pending.add(paths.root());
        try (ExecutorService tasks = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                ExecutorCompletionService<Resource> completed = new ExecutorCompletionService<>(tasks);
                int active = 0;
                while (!pending.isEmpty() || active > 0) {
                    while (active < parallelism && !pending.isEmpty()) {
                        URI uri = pending.remove();
                        completed.submit(() -> fetcher.fetch(uri));
                        active++;
                    }
                    Resource resource = completed.take().get();
                    active--;
                    messages.add(resource.message());
                    for (String successor : resource.successors()) {
                        URI uri = paths.resolve(successor);
                        if (visited.add(uri)) {
                            pending.add(uri);
                        }
                    }
                }
            } catch (ExecutionException e) {
                throw new IOException("Resource fetch failed", e.getCause());
            } finally {
                tasks.shutdownNow();
            }
        }
        messages.sort(String::compareTo);
        return List.copyOf(messages);
    }
}
