package ru.nsu.masolygin.xml.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.masolygin.xml.write.PeopleWriter;

class PeopleReaderTest {

    @TempDir
    Path directory;

    private Path input(String xml) throws java.io.IOException {
        Path path = directory.resolve("input.xml");
        Files.writeString(path, xml);
        return path;
    }

    private String family() {
        return (
            "<people count='3'>" +
            "<person id='P1' name=' Alice  Example '><gender>F</gender><children-number value='1'/><siblings-number value='0'/><husband value='P2'/></person>" +
            "<person id='P2'><fullname><first>Bob</first><family>Example</family></fullname><children-number value='1'/></person>" +
            "<person name='Alice Example'><children><son id='P3'/></children></person>" +
            "<person id='P3'><firstname value='Charlie'/><family-name>Example</family-name><mother>Alice Example</mother><father>Bob Example</father></person>" +
            "<person name='Charlie Example'><parent value='UNKNOWN'/></person></people>"
        );
    }

    @Test
    void testFragmentsAndInverseRelations()
        throws java.io.IOException, org.xml.sax.SAXException, javax.xml.parsers.ParserConfigurationException, javax.xml.stream.XMLStreamException {
        var people = new PeopleReader().read(input(family()));
        assertEquals(3, people.persons().size());
        assertEquals(5, people.sourceFragments());
        assertEquals(
            Set.of("P1", "P2"),
            people.persons().get("P3").relatives("parents")
        );
        assertEquals(
            Set.of("P3"),
            people.persons().get("P2").relatives("children")
        );
        assertEquals("male", people.persons().get("P3").gender());
        assertEquals(
            Set.of("P1"),
            people.persons().get("P2").relatives("marriage")
        );
        Path output = directory.resolve("normalized.xml");
        new PeopleWriter().write(people, output);
        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(output.toFile());
        assertEquals(3, document.getElementsByTagName("person").getLength());
        assertEquals(2, document.getElementsByTagName("son").getLength());
    }

    @Test
    void testNameCollisionAndAbsentSpouse()
        throws java.io.IOException, org.xml.sax.SAXException, javax.xml.parsers.ParserConfigurationException {
        String xml =
            "<people count='3'>" +
            "<person id='P1' name='Sam Twin'><gender>M</gender></person>" +
            "<person id='P2' name='Sam Twin'><gender>M</gender><spouce/></person>" +
            "<person id='P3' name='Pat Partner'><gender>F</gender><spouce value='Sam Twin'/></person>" +
            "<person name='Sam Twin'><wife value='P3'/></person></people>";
        var people = new PeopleReader().read(input(xml));
        assertEquals(
            Set.of("P3"),
            people.persons().get("P1").relatives("marriage")
        );
        assertEquals(
            Set.of(),
            people.persons().get("P2").relatives("marriage")
        );
        assertEquals(3, people.persons().size());
    }

    @Test
    void testCountAndUnknownReferenceErrors() throws java.io.IOException {
        Path wrongCount = input(
            "<people count='2'><person id='P1' name='A One'/></people>"
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PeopleReader().read(wrongCount)
        );
        Path wrongChildren = input(
            "<people count='1'><person id='P1' name='A One'><children-number value='1'/></person></people>"
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PeopleReader().read(wrongChildren)
        );
        Path missing = input(
            "<people count='1'><person id='P1' name='A One'><father>Missing Person</father></person></people>"
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PeopleReader().read(missing)
        );
    }

    @Test
    void testZipInput()
        throws java.io.IOException, org.xml.sax.SAXException, javax.xml.parsers.ParserConfigurationException {
        Path archive = directory.resolve("people.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("people.xml"));
            zip.write(family().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertEquals(3, new PeopleReader().read(archive).persons().size());
    }
}
