package ru.nsu.masolygin.xml.parse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class FragmentHandler extends DefaultHandler {

    private record Element(
        String tag,
        Map<String, String> attributes,
        StringBuilder text
    ) {}

    private final Deque<Element> stack = new ArrayDeque<>();
    private final Consumer<Fragment> consumer;
    private Fragment current;
    int expectedCount = -1;
    int fragments;

    FragmentHandler(Consumer<Fragment> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void startElement(
        String uri,
        String local,
        String tag,
        Attributes attributes
    ) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) values.put(
            attributes.getQName(i),
            attributes.getValue(i)
        );
        stack.push(new Element(tag, values, new StringBuilder()));
        if (tag.equals("people")) expectedCount = Integer.parseInt(
            values.getOrDefault("count", "-1")
        );
        if (tag.equals("person")) {
            current = new Fragment();
            current.id = values.get("id");
            String name = normalize(values.get("name"));
            if (!name.isEmpty()) {
                String[] parts = name.split(" ", 2);
                if (parts.length != 2) throw new IllegalArgumentException(
                    "Incomplete name: " + name
                );
                current.field("first-name", parts[0]);
                current.field("last-name", parts[1]);
            }
        }
    }

    @Override
    public void characters(char[] text, int start, int length) {
        if (!stack.isEmpty()) stack.peek().text().append(text, start, length);
    }

    @Override
    public void endElement(String uri, String local, String tag) {
        Element element = stack.pop();
        if (tag.equals("person")) {
            consumer.accept(current);
            fragments++;
            current = null;
            return;
        }
        if (current == null) return;
        String value = element.text().toString();
        for (String key : new String[] { "id", "val", "value" }) {
            if (element.attributes().containsKey(key)) value = element
                .attributes()
                .get(key);
        }
        value = normalize(value);
        switch (tag) {
            case "id" -> {
                if (
                    current.id != null && !current.id.equals(value)
                ) throw new IllegalArgumentException("Conflicting ID");
                current.id = value;
            }
            case "first", "firstname" -> current.field("first-name", value);
            case "family", "surname", "family-name" -> current.field(
                "last-name",
                value
            );
            case "gender" -> {
                String gender = switch (value.toLowerCase(Locale.ROOT)) {
                    case "m", "male" -> "male";
                    case "f", "female" -> "female";
                    default -> throw new IllegalArgumentException(
                        "Unknown gender: " + value
                    );
                };
                current.field("gender", gender);
            }
            case "children-number", "siblings-number" -> {
                int count = Integer.parseInt(value);
                if (count < 0) throw new IllegalArgumentException(
                    "Negative count"
                );
                current.field(
                    tag.replace("-number", "-count"),
                    Integer.toString(count)
                );
            }
            case "siblings" -> {
                if (!value.isEmpty()) for (String id : value.split(" "))
                    current.relation("sibling", id);
            }
            case "spouce", "spouse", "wife", "husband" -> {
                if (value.isEmpty() || value.equals("NONE")) current.field(
                    "no-spouse",
                    "true"
                );
                else current.relation(tag, value);
            }
            case
                "parent",
                "father",
                "mother",
                "child",
                "son",
                "daughter",
                "brother",
                "sister" -> {
                if (
                    !value.isEmpty() && !value.equals("UNKNOWN")
                ) current.relation(tag, value);
            }
            default -> {
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
