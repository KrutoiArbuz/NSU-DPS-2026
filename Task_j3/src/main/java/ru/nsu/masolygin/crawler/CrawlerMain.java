package ru.nsu.masolygin.crawler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import ru.nsu.masolygin.cli.Arguments;
import ru.nsu.masolygin.http.HttpResourceFetcher;

public class CrawlerMain {
    public static void main(String[] args) {
        try {
            Arguments options = new Arguments(args, Set.of("server", "parallel", "timeout-seconds", "output"));
            URI server = URI.create(options.optional("server", "http://localhost:8080"));
            int parallel = Integer.parseInt(options.optional("parallel", "256"));
            Duration timeout = Duration.ofSeconds(Long.parseLong(options.optional("timeout-seconds", "30")));
            List<String> messages;
            try (HttpResourceFetcher fetcher = new HttpResourceFetcher(timeout)) {
                messages = new Crawler(fetcher, server, parallel).crawl();
            }
            String output = options.optional("output", "");
            if (output.isEmpty()) {
                for (String message : messages) {
                    System.out.println(message);
                }
            } else {
                Files.write(Path.of(output), messages, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                System.err.println("Saved " + messages.size() + " messages to " + output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Crawl interrupted");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Crawler error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
        }
    }
}
