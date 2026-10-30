package ru.nsu.masolygin.xml.write;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.binding.PeopleDocument;
import ru.nsu.masolygin.xml.binding.PersonEntry;
import ru.nsu.masolygin.xml.model.People;
import ru.nsu.masolygin.xml.model.Person;
import ru.nsu.masolygin.xml.schema.PeopleSchema;

public class JaxbPeopleWriter {

    public void write(People people, Path output)
        throws JAXBException, SAXException, IOException {
        PeopleDocument document = new PeopleDocument();
        document.count = people.persons().size();
        Map<String, PersonEntry> entries = new LinkedHashMap<>();
        var persons = people
            .persons()
            .values()
            .stream()
            .sorted(Comparator.comparing(Person::id))
            .toList();
        for (Person person : persons) {
            PersonEntry entry = new PersonEntry();
            entry.id = person.id();
            entry.firstName = person.field("first-name");
            entry.lastName = person.field("last-name");
            entry.gender = person.gender();
            entry.childrenCount = person.relatives("children").size();
            entry.siblingsCount = person.relatives("siblings").size();
            entries.put(entry.id, entry);
            document.persons.add(entry);
        }
        PeopleWriter roles = new PeopleWriter();
        for (Person person : persons) {
            PersonEntry entry = entries.get(person.id());
            for (String kind : new String[] {
                "parents",
                "marriage",
                "siblings",
                "children",
            }) {
                for (String id : person.relatives(kind)) {
                    String role = roles.role(
                        kind,
                        people.persons().get(id).gender()
                    );
                    entry.group(kind).add(role, entries.get(id));
                }
            }
        }
        var marshaller = JAXBContext.newInstance(
            PeopleDocument.class
        ).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setSchema(new PeopleSchema().load());
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (var stream = Files.newOutputStream(output)) {
            marshaller.marshal(document, stream);
        }
        new PeopleSchema().validate(output);
    }
}
