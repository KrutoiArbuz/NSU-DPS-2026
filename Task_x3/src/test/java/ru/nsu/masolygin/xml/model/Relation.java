package ru.nsu.masolygin.xml.model;

import java.util.Locale;

public record Relation(String tag, String target) {
    public String kind() {
        return switch (tag) {
            case "parent", "father", "mother" -> "parents";
            case "child", "son", "daughter" -> "children";
            case "sibling", "brother", "sister" -> "siblings";
            case "spouce", "spouse", "wife", "husband" -> "marriage";
            default -> throw new IllegalArgumentException(
                "Unknown relation: " + tag
            );
        };
    }

    public String inverse() {
        return switch (kind()) {
            case "parents" -> "children";
            case "children" -> "parents";
            default -> kind();
        };
    }

    public String targetGender() {
        return switch (tag.toLowerCase(Locale.ROOT)) {
            case "father", "son", "brother", "husband" -> "male";
            case "mother", "daughter", "sister", "wife" -> "female";
            default -> null;
        };
    }
}
