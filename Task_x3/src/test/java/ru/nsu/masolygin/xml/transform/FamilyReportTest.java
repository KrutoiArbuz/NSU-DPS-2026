package ru.nsu.masolygin.xml.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.masolygin.xml.model.People;
import ru.nsu.masolygin.xml.model.Person;
import ru.nsu.masolygin.xml.model.Relation;
import ru.nsu.masolygin.xml.write.JaxbPeopleWriter;

class FamilyReportTest {

    @TempDir
    Path directory;

    private Person person(String id, String firstName, String gender) {
        Person person = new Person(id);
        person.merge(
            Map.of(
                "first-name",
                firstName,
                "last-name",
                "Family",
                "gender",
                gender
            )
        );
        return person;
    }

    @Test
    void testSelectedFamilyAndAllRequestedRelatives()
        throws java.io.IOException, jakarta.xml.bind.JAXBException, org.xml.sax.SAXException, javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException {
        Person alice = person("P1", "Alice", "female");
        Person bob = person("P2", "Bob", "male");
        Person father = person("P3", "Father", "male");
        Person mother = person("P4", "Mother", "female");
        Person grandma = person("P5", "Grandma", "female");
        Person grandpa = person("P6", "Grandpa", "male");
        Person uncle = person("P7", "Uncle", "male");
        Person aunt = person("P8", "Aunt", "female");
        for (Person child : new Person[] { alice, bob }) {
            child.connect(new Relation("father", father.id()), father);
            child.connect(new Relation("mother", mother.id()), mother);
        }
        alice.connect(new Relation("brother", bob.id()), bob);
        father.connect(new Relation("mother", grandma.id()), grandma);
        mother.connect(new Relation("father", grandpa.id()), grandpa);
        father.connect(new Relation("brother", uncle.id()), uncle);
        mother.connect(new Relation("sister", aunt.id()), aunt);
        Map<String, Person> persons = new TreeMap<>();
        for (Person person : new Person[] {
            alice,
            bob,
            father,
            mother,
            grandma,
            grandpa,
            uncle,
            aunt,
        })
            persons.put(person.id(), person);
        Path xml = directory.resolve("people.xml");
        new JaxbPeopleWriter().write(new People(persons, 8), xml);
        Path html = directory.resolve("family.html");
        new FamilyReport().write(xml, html);
        String result = Files.readString(html);
        assertEquals(4, result.split("<section>", -1).length - 1);
        String selectedCard = result.substring(
            result.indexOf("<section>"),
            result.indexOf("</section>")
        );
        for (String name : new String[] {
            "Alice Family",
            "Bob Family",
            "Father Family",
            "Mother Family",
            "Grandma Family",
            "Grandpa Family",
            "Uncle Family",
            "Aunt Family",
        })
            assertTrue(selectedCard.contains(name), name);
        assertTrue(selectedCard.contains("женский"));
        assertTrue(result.contains("Сыновья") && result.contains("Дочери"));
    }

    @Test
    void testNoMatchingPersonIsExplicit()
        throws java.io.IOException, jakarta.xml.bind.JAXBException, org.xml.sax.SAXException, javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException {
        Person alone = person("P1", "Alone", "male");
        Path xml = directory.resolve("people.xml");
        new JaxbPeopleWriter().write(new People(Map.of("P1", alone), 1), xml);
        Path html = directory.resolve("family.html");
        new FamilyReport().write(xml, html);
        String result = Files.readString(html);
        assertTrue(result.contains("Нет человека"));
        assertTrue(!result.contains("<section>"));
    }
}
