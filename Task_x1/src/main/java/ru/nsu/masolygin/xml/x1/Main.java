package ru.nsu.masolygin.xml.x1;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.cli.Arguments;
import ru.nsu.masolygin.xml.parse.PeopleReader;
import ru.nsu.masolygin.xml.write.PeopleWriter;

public class Main {

    public static void main(String[] args)
        throws IOException, SAXException, ParserConfigurationException, XMLStreamException {
        Arguments arguments = new Arguments(args);
        var people = new PeopleReader().read(
            arguments.path("--input", "task_X1.zip")
        );
        var output = arguments.path("--output", "build/people.xml");
        new PeopleWriter().write(people, output);
        System.out.println(
            "Fragments: " +
                people.sourceFragments() +
                ", people: " +
                people.persons().size() +
                ", output: " +
                output
        );
    }
}
