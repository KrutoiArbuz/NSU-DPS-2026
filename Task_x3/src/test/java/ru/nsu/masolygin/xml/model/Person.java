package ru.nsu.masolygin.xml.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Person {

    private final String id;
    private final Map<String, String> fields = new LinkedHashMap<>();
    private final Map<String, Set<String>> links = new LinkedHashMap<>();

    public Person(String id) {
        this.id = id;
        for (String kind : new String[] {
            "parents",
            "marriage",
            "siblings",
            "children",
        }) {
            links.put(kind, new TreeSet<>());
        }
    }

    public String id() {
        return id;
    }

    public String field(String name) {
        return fields.get(name);
    }

    public String name() {
        return field("first-name") + " " + field("last-name");
    }

    public String gender() {
        return fields.getOrDefault("gender", "unknown");
    }

    public Set<String> relatives(String kind) {
        return Collections.unmodifiableSet(links.get(kind));
    }

    public boolean accepts(Map<String, String> data) {
        for (var entry : data.entrySet()) {
            String known = fields.get(entry.getKey());
            if (known != null && !known.equals(entry.getValue())) return false;
        }
        if (
            data.containsKey("no-spouse") && !links.get("marriage").isEmpty()
        ) return false;
        for (String kind : new String[] { "children", "siblings" }) {
            String count = data.get(kind + "-count");
            if (
                count != null &&
                links.get(kind).size() > Integer.parseInt(count)
            ) return false;
        }
        return true;
    }

    public void merge(Map<String, String> data) {
        if (!accepts(data)) throw new IllegalArgumentException(
            "Conflicting fields for " + id + ": " + data
        );
        fields.putAll(data);
    }

    public boolean represents(Map<String, String> data) {
        for (var entry : data.entrySet()) {
            String known = fields.get(entry.getKey());
            if (known == null && entry.getKey().endsWith("-count")) {
                String kind = entry.getKey().replace("-count", "");
                known = Integer.toString(links.get(kind).size());
            }
            if (!entry.getValue().equals(known)) return false;
        }
        return true;
    }

    public boolean full(String kind) {
        String count = fields.get(kind + "-count");
        int limit =
            count != null
                ? Integer.parseInt(count)
                : switch (kind) {
                      case "parents" -> 2;
                      case "marriage" -> fields.containsKey("no-spouse")
                          ? 0
                          : 1;
                      default -> Integer.MAX_VALUE;
                  };
        return links.get(kind).size() >= limit;
    }

    public void connect(Relation relation, Person other) {
        if (id.equals(other.id)) throw new IllegalArgumentException(
            "Self relation: " + id
        );
        if (relation.targetGender() != null) other.merge(
            Map.of("gender", relation.targetGender())
        );
        links.get(relation.kind()).add(other.id);
        other.links.get(relation.inverse()).add(id);
    }

    public void validate() {
        if (
            field("first-name") == null || field("last-name") == null
        ) throw new IllegalArgumentException("Missing name: " + id);
        for (String kind : new String[] { "children", "siblings" }) {
            String count = fields.get(kind + "-count");
            if (
                count != null &&
                Integer.parseInt(count) != links.get(kind).size()
            ) throw new IllegalArgumentException(
                "Wrong " + kind + " count: " + id
            );
        }
        if (
            links.get("parents").size() > 2 || links.get("marriage").size() > 1
        ) throw new IllegalArgumentException("Too many parents/spouses: " + id);
        if (
            fields.containsKey("no-spouse") && !links.get("marriage").isEmpty()
        ) throw new IllegalArgumentException("Unexpected spouse: " + id);
    }
}
