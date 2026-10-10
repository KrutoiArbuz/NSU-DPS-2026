package ru.nsu.masolygin.http;

import java.util.List;

public record Resource(String message, List<String> successors) {
    public Resource {
        successors = List.copyOf(successors);
    }
}
