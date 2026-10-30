package ru.nsu.masolygin.xml.write;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import ru.nsu.masolygin.xml.model.People;
import ru.nsu.masolygin.xml.model.Person;

public class PeopleWriter {

    public void write(People people, Path output)
        throws IOException, XMLStreamException {
        Path parent = output.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        try (var stream = Files.newOutputStream(output)) {
            var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(
                stream,
                "UTF-8"
            );
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("people");
            writer.writeAttribute(
                "count",
                Integer.toString(people.persons().size())
            );
            var persons = people
                .persons()
                .values()
                .stream()
                .sorted(Comparator.comparing(Person::id))
                .toList();
            for (Person person : persons) {
                writer.writeStartElement("person");
                writer.writeAttribute("id", person.id());
                writer.writeAttribute(
                    "children-count",
                    Integer.toString(person.relatives("children").size())
                );
                writer.writeAttribute(
                    "siblings-count",
                    Integer.toString(person.relatives("siblings").size())
                );
                for (String field : new String[] {
                    "first-name",
                    "last-name",
                    "gender",
                }) {
                    writer.writeStartElement(field);
                    writer.writeCharacters(
                        field.equals("gender")
                            ? person.gender()
                            : person.field(field)
                    );
                    writer.writeEndElement();
                }
                for (String kind : new String[] {
                    "parents",
                    "marriage",
                    "siblings",
                    "children",
                }) {
                    writer.writeStartElement(kind);
                    for (String gender : new String[] {
                        "male",
                        "female",
                        "unknown",
                    }) {
                        for (String id : person.relatives(kind)) {
                            Person target = people.persons().get(id);
                            if (!target.gender().equals(gender)) continue;
                            writer.writeEmptyElement(role(kind, gender));
                            writer.writeAttribute("ref", id);
                        }
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        }
    }

    public String role(String kind, String gender) {
        return switch (kind) {
            case "parents" -> gender.equals("male")
                ? "father"
                : gender.equals("female")
                  ? "mother"
                  : "parent";
            case "children" -> gender.equals("male")
                ? "son"
                : gender.equals("female")
                  ? "daughter"
                  : "child";
            case "siblings" -> gender.equals("male")
                ? "brother"
                : gender.equals("female")
                  ? "sister"
                  : "sibling";
            case "marriage" -> gender.equals("male")
                ? "husband"
                : gender.equals("female")
                  ? "wife"
                  : "spouse";
            default -> throw new IllegalArgumentException(
                "Unknown kind: " + kind
            );
        };
    }
}
