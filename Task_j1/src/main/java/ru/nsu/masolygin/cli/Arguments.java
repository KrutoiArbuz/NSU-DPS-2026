package ru.nsu.masolygin.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Arguments {
    private final Map<String, String> values = new HashMap<>();

    public Arguments(String[] args, Set<String> options) {
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (!option.startsWith("--") || !options.contains(option.substring(2))) {
                throw new IllegalArgumentException("Unknown option: " + option);
            }

            String name = option.substring(2);

            if (values.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate option: " + option);
            }

            String value;
            if (name.equals("abort")) {
                value = "true";
            } else {
                if (++i == args.length || args[i].startsWith("--")) {
                    throw new IllegalArgumentException("Missing value for " + option);
                }
                value = args[i];
            }

            values.put(name, value);
        }
    }

    public String required(String name) {
        String value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Required option: --" + name);
        }
        return value;
    }

    public String optional(String name, String defaultValue) {
        return values.getOrDefault(name, defaultValue);
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

}
