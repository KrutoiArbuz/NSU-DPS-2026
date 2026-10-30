package ru.nsu.masolygin.xml.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.nsu.masolygin.xml.model.Relation;

class Fragment {

    String id;
    final Map<String, String> fields = new LinkedHashMap<>();
    final List<Relation> relations = new ArrayList<>();

    void field(String name, String value) {
        String old = fields.putIfAbsent(name, value);
        if (
            old != null && !old.equals(value)
        ) throw new IllegalArgumentException(
            "Conflicting fragment field: " + name
        );
    }

    void relation(String tag, String value) {
        relations.add(new Relation(tag, value));
        if (tag.equals("wife")) field("gender", "male");
        if (tag.equals("husband")) field("gender", "female");
    }

    String name() {
        return fields.get("first-name") + " " + fields.get("last-name");
    }
}
