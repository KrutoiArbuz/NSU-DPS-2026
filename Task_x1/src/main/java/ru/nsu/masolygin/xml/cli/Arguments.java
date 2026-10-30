package ru.nsu.masolygin.xml.cli;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Arguments {

    private final Map<String, String> values = new HashMap<>();

    public Arguments(String[] args) {
        for (int i = 0; i < args.length; i += 2) {
            if (
                !args[i].equals("--input") && !args[i].equals("--output")
            ) throw new IllegalArgumentException("Unknown flag: " + args[i]);
            if (i + 1 == args.length) throw new IllegalArgumentException(
                "Missing value: " + args[i]
            );
            if (
                values.putIfAbsent(args[i], args[i + 1]) != null
            ) throw new IllegalArgumentException("Repeated flag: " + args[i]);
        }
    }

    public Path path(String flag, String fallback) {
        return Path.of(values.getOrDefault(flag, fallback));
    }
}
