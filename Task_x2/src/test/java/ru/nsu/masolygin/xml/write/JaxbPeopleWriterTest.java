package ru.nsu.masolygin.xml.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.xml.bind.JAXBContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.binding.PeopleDocument;
import ru.nsu.masolygin.xml.model.People;
import ru.nsu.masolygin.xml.model.Person;
import ru.nsu.masolygin.xml.model.Relation;
import ru.nsu.masolygin.xml.schema.PeopleSchema;

class JaxbPeopleWriterTest {

    @TempDir
    Path directory;

    private Path family()
        throws java.io.IOException, jakarta.xml.bind.JAXBException, SAXException {
        Person mother = new Person("P1");
        mother.merge(
            Map.of(
                "first-name",
                "Alice",
                "last-name",
                "Example",
                "gender",
                "female"
            )
        );
        Person father = new Person("P2");
        father.merge(
            Map.of(
                "first-name",
                "Bob",
                "last-name",
                "Example",
                "gender",
                "male"
            )
        );
        Person child = new Person("P3");
        child.merge(
            Map.of(
                "first-name",
                "Charlie",
                "last-name",
                "Example",
                "gender",
                "male"
            )
        );
        child.connect(new Relation("mother", "P1"), mother);
        child.connect(new Relation("father", "P2"), father);
        Path output = directory.resolve("people.xml");
        new JaxbPeopleWriter().write(
            new People(Map.of("P1", mother, "P2", father, "P3", child), 3),
            output
        );
        return output;
    }

    @Test
    void testSchemaAndIdrefRoundTrip()
        throws java.io.IOException, jakarta.xml.bind.JAXBException, SAXException, javax.xml.parsers.ParserConfigurationException {
        Path file = family();
        var unmarshaller = JAXBContext.newInstance(
            PeopleDocument.class
        ).createUnmarshaller();
        unmarshaller.setSchema(new PeopleSchema().load());
        PeopleDocument document = (PeopleDocument) unmarshaller.unmarshal(
            file.toFile()
        );
        assertEquals(3, document.count);
        assertSame(
            document.persons.get(0),
            document.persons.get(2).parents.mother.getFirst().person
        );
        assertSame(
            document.persons.get(2),
            document.persons.get(0).children.son.getFirst().person
        );
        assertEquals(
            "Alice",
            new PeopleSchema()
                .document(file)
                .getElementById("P1")
                .getElementsByTagName("first-name")
                .item(0)
                .getTextContent()
        );
    }

    @Test
    void testRejectInvalidGenderDuplicateIdAndDanglingIdref()
        throws java.io.IOException, jakarta.xml.bind.JAXBException, SAXException {
        String original = Files.readString(family());
        for (String bad : new String[] {
            original.replace("<gender>male</gender>", "<gender>other</gender>"),
            original.replace("id=\"P2\"", "id=\"P1\""),
            original.replace("ref=\"P1\"", "ref=\"P999\""),
        }) {
            Path path = directory.resolve("bad.xml");
            Files.writeString(path, bad);
            assertThrows(SAXException.class, () ->
                new PeopleSchema().validate(path)
            );
        }
    }
}
