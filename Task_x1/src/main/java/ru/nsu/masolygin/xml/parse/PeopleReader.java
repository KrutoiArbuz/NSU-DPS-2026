package ru.nsu.masolygin.xml.parse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.model.People;
import ru.nsu.masolygin.xml.model.Person;
import ru.nsu.masolygin.xml.model.Relation;

public class PeopleReader {

    private record Pending(Person owner, Relation relation) {}

    private record Deferred(Fragment fragment, List<Person> candidates) {}

    public People read(Path path)
        throws IOException, SAXException, ParserConfigurationException {
        Map<String, Person> people = new TreeMap<>();
        FragmentHandler first = parse(path, fragment -> {
            if (fragment.id != null) people
                .computeIfAbsent(fragment.id, Person::new)
                .merge(fragment.fields);
        });
        Map<String, List<Person>> names = new HashMap<>();
        for (Person person : people.values()) {
            if (
                person.field("first-name") == null ||
                person.field("last-name") == null
            ) throw new IllegalArgumentException(
                "ID has no complete name: " + person.id()
            );
            names
                .computeIfAbsent(person.name(), ignored -> new ArrayList<>())
                .add(person);
        }
        Set<Pending> pending = new LinkedHashSet<>();
        List<Deferred> deferred = new ArrayList<>();
        parse(path, fragment -> {
            List<Person> candidates =
                fragment.id != null
                    ? List.of(people.get(fragment.id))
                    : names.getOrDefault(fragment.name(), List.of());
            if (candidates.isEmpty()) throw new IllegalArgumentException(
                "Unknown person: " + fragment.name()
            );
            if (candidates.size() == 1) absorb(
                candidates.getFirst(),
                fragment,
                pending
            );
            else deferred.add(new Deferred(fragment, candidates));
        });
        boolean changed;
        do {
            changed = false;
            var edges = pending.iterator();
            while (edges.hasNext()) {
                Pending edge = edges.next();
                List<Person> candidates = targets(
                    edge.owner(),
                    edge.relation(),
                    people,
                    names
                );
                if (candidates.isEmpty()) throw new IllegalArgumentException(
                    "Unresolvable relation: " + edge
                );
                if (candidates.size() == 1) {
                    edge.owner().connect(
                        edge.relation(),
                        candidates.getFirst()
                    );
                    edges.remove();
                    changed = true;
                }
            }
            var fragments = deferred.iterator();
            while (fragments.hasNext()) {
                Deferred item = fragments.next();
                List<Person> candidates = item
                    .candidates()
                    .stream()
                    .filter(person ->
                        compatible(person, item.fragment(), people, names)
                    )
                    .toList();
                if (candidates.isEmpty()) throw new IllegalArgumentException(
                    "Conflicting fragment: " + item.fragment().name()
                );
                if (candidates.size() == 1) {
                    absorb(candidates.getFirst(), item.fragment(), pending);
                    fragments.remove();
                    changed = true;
                }
            }
        } while (changed);
        if (!pending.isEmpty()) throw new IllegalArgumentException(
            "Ambiguous relatives: " + pending
        );
        for (Deferred item : deferred) {
            for (Person person : item.candidates()) {
                if (
                    !item.fragment().relations.isEmpty() ||
                    !person.represents(item.fragment().fields)
                ) throw new IllegalArgumentException(
                    "Ambiguous person: " + item.fragment().name()
                );
            }
        }
        if (
            first.expectedCount >= 0 && people.size() != first.expectedCount
        ) throw new IllegalArgumentException(
            "Wrong people count: " + people.size()
        );
        for (Person person : people.values()) person.validate();
        return new People(people, first.fragments);
    }

    private void absorb(
        Person person,
        Fragment fragment,
        Set<Pending> pending
    ) {
        person.merge(fragment.fields);
        for (Relation relation : fragment.relations)
            pending.add(new Pending(person, relation));
    }

    private boolean compatible(
        Person person,
        Fragment fragment,
        Map<String, Person> people,
        Map<String, List<Person>> names
    ) {
        if (!person.accepts(fragment.fields)) return false;
        for (Relation relation : fragment.relations)
            if (
                targets(person, relation, people, names).isEmpty()
            ) return false;
        return true;
    }

    private List<Person> targets(
        Person owner,
        Relation relation,
        Map<String, Person> people,
        Map<String, List<Person>> names
    ) {
        List<Person> raw = people.containsKey(relation.target())
            ? List.of(people.get(relation.target()))
            : names.getOrDefault(relation.target(), List.of());
        List<Person> result = new ArrayList<>();
        for (Person target : raw) {
            if (owner.id().equals(target.id())) continue;
            String gender = relation.targetGender();
            if (
                gender != null &&
                !target.gender().equals("unknown") &&
                !target.gender().equals(gender)
            ) continue;
            if (
                owner.full(relation.kind()) &&
                !owner.relatives(relation.kind()).contains(target.id())
            ) continue;
            if (
                target.full(relation.inverse()) &&
                !target.relatives(relation.inverse()).contains(owner.id())
            ) continue;
            result.add(target);
        }
        List<Person> linked = result
            .stream()
            .filter(target ->
                owner.relatives(relation.kind()).contains(target.id())
            )
            .toList();
        return linked.isEmpty() ? result : linked;
    }

    private FragmentHandler parse(Path path, Consumer<Fragment> consumer)
        throws IOException, SAXException, ParserConfigurationException {
        FragmentHandler handler = new FragmentHandler(consumer);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature(
            "http://apache.org/xml/features/disallow-doctype-decl",
            true
        );
        factory.setFeature(
            "http://xml.org/sax/features/external-general-entities",
            false
        );
        factory.setFeature(
            "http://xml.org/sax/features/external-parameter-entities",
            false
        );
        if (path.toString().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(path.toFile())) {
                var entry = zip.getEntry("people.xml");
                if (entry == null) throw new IOException(
                    "ZIP has no people.xml"
                );
                try (InputStream input = zip.getInputStream(entry)) {
                    factory.newSAXParser().parse(input, handler);
                }
            }
        } else {
            try (InputStream input = Files.newInputStream(path)) {
                factory.newSAXParser().parse(input, handler);
            }
        }
        return handler;
    }
}
